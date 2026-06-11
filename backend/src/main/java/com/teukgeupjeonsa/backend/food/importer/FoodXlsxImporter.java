package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.food.*;
import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FoodXlsxImporter {

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final ManualFoodOverrideRepository manualFoodOverrideRepository;
    private final ServingDefaultRepository servingDefaultRepository;
    private final FoodNameNormalizer foodNameNormalizer;
    private final EntityManager entityManager;
    private final DataSource dataSource;

    @Transactional
    public FoodImportResult importXlsx(Path xlsxPath) {
        if (!Files.exists(xlsxPath)) {
            throw new IllegalArgumentException("식품 xlsx 파일을 찾을 수 없습니다: " + xlsxPath.toAbsolutePath());
        }

        try (InputStream inputStream = Files.newInputStream(xlsxPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FoodReadResult foodReadResult = readFoods(workbook);

            manualFoodOverrideRepository.deleteAllInBatch();
            servingDefaultRepository.deleteAllInBatch();
            foodAliasRepository.deleteAllInBatch();
            detachUserMealFoodReferencesIfPresent();
            foodRepository.deleteAllInBatch();
            foodRepository.flush();

            List<Food> savedFoods = foodRepository.saveAll(foodReadResult.foods());
            foodRepository.flush();
            Map<String, Food> foodByName = toFoodByNormalizedName(savedFoods);

            AliasReadResult aliasReadResult = readAliases(workbook, foodByName);
            foodAliasRepository.saveAll(aliasReadResult.aliases());

            int servingDefaultCount = importServingDefaults(workbook);
            int overrideCount = importManualOverrides(workbook, foodByName);

            return new FoodImportResult(savedFoods.size(), aliasReadResult.aliases().size(),
                    foodReadResult.skippedCount(), aliasReadResult.skippedCount(), overrideCount, servingDefaultCount);
        } catch (IOException e) {
            throw new IllegalStateException("식품 xlsx 파일을 읽는 중 오류가 발생했습니다: " + xlsxPath.toAbsolutePath(), e);
        }
    }

    private FoodReadResult readFoods(Workbook workbook) {
        Sheet sheet = getRequiredSheet(workbook, "food_master_clean_100g", "food_master");
        Map<String, Integer> columns = readHeader(sheet, sheet.getSheetName());

        Map<String, Food> foodsByName = new LinkedHashMap<>();
        int skippedCount = 0;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String name = maxLength(text(row, firstColumn(columns, "representative_name", "food_name", "name", "final_food_name")), 200);
            if (isInvalidFoodName(name)) {
                skippedCount++;
                continue;
            }
            String category = maxLength(text(row, firstColumn(columns, "display_category", "category")), 80);
            String servingUnit = maxLength(Optional.ofNullable(text(row, firstColumn(columns, "basis", "serving_unit"))).orElse("100g"), 20);
            Food food = Food.builder()
                    .name(name)
                    .searchName(maxLength(foodNameNormalizer.toSearchName(name), 200))
                    .category(category)
                    .servingUnit(servingUnit)
                    .calorie(number(row, firstColumn(columns, "kcal_per_100g", "calorie_kcal", "calories", "kcal")))
                    .carbohydrate(number(row, firstColumn(columns, "carbohydrate_per_100g", "carbohydrate_g", "carb_g")))
                    .protein(number(row, firstColumn(columns, "protein_per_100g", "protein_g")))
                    .fat(number(row, firstColumn(columns, "fat_per_100g", "fat_g")))
                    .sugar(number(row, firstColumn(columns, "sugar_g")))
                    .sodium(number(row, firstColumn(columns, "sodium_mg")))
                    .cholesterol(number(row, firstColumn(columns, "cholesterol_mg")))
                    .saturatedFat(number(row, firstColumn(columns, "saturated_fat_g")))
                    .transFat(number(row, firstColumn(columns, "trans_fat_g")))
                    .sourceCount(integer(row, firstColumn(columns, "alias_count", "source_count")))
                    .qualityFlag(maxLength(text(row, firstColumn(columns, "quality_flag")), 40))
                    .build();

            String dedupeKey = dedupeKey(name);
            Food previous = foodsByName.get(dedupeKey);
            if (previous == null || hasGreaterSourceCount(food, previous)) {
                foodsByName.put(dedupeKey, food);
            }
            if (previous != null) skippedCount++;
        }
        return new FoodReadResult(new ArrayList<>(foodsByName.values()), skippedCount);
    }

    private AliasReadResult readAliases(Workbook workbook, Map<String, Food> foodByName) {
        Sheet sheet = workbook.getSheet("food_alias");
        if (sheet == null) return new AliasReadResult(List.of(), 0);
        Map<String, Integer> columns = readHeader(sheet, "food_alias");
        List<FoodAlias> aliases = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skippedCount = 0;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String aliasName = maxLength(text(row, firstColumn(columns, "raw_food_name", "alias_name", "original_name")), 300);
            if (aliasName == null) { skippedCount++; continue; }
            String mappedFoodName = maxLength(text(row, firstColumn(columns, "representative_name", "final_food_name", "food_name")), 200);
            if (mappedFoodName == null) mappedFoodName = aliasName;
            Food food = foodByName.get(dedupeKey(mappedFoodName));
            if (food == null) { skippedCount++; continue; }
            String originalName = Optional.ofNullable(text(row, firstColumn(columns, "original_name"))).orElse(aliasName);
            String key = food.getName() + "\n" + dedupeKey(aliasName) + "\n" + dedupeKey(originalName);
            if (!seen.add(key)) continue;
            aliases.add(FoodAlias.builder()
                    .food(food)
                    .aliasName(aliasName)
                    .searchName(maxLength(foodNameNormalizer.toSearchName(aliasName), 300))
                    .originalName(maxLength(originalName, 300))
                    .category(maxLength(text(row, firstColumn(columns, "display_category", "category")), 80))
                    .build());
        }
        return new AliasReadResult(aliases, skippedCount);
    }

    private int importManualOverrides(Workbook workbook, Map<String, Food> foodByName) {
        Sheet sheet = workbook.getSheet("manual_overrides");
        if (sheet == null) return 0;
        Map<String, Integer> columns = readHeader(sheet, "manual_overrides");
        Map<String, ManualFoodOverride> overridesByNormalizedName = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String rawMenu = text(row, firstColumn(columns, "raw_menu_name", "menu_name"));
            String representativeName = text(row, firstColumn(columns, "representative_name", "food_name", "final_food_name"));
            if (rawMenu == null || representativeName == null) continue;
            Food food = foodByName.get(dedupeKey(representativeName));
            if (food == null) continue;
            String normalizedMenuName = maxLength(foodNameNormalizer.toSearchName(rawMenu), 200);
            overridesByNormalizedName.putIfAbsent(normalizedMenuName, ManualFoodOverride.builder()
                    .rawMenuName(maxLength(rawMenu, 200))
                    .normalizedMenuName(normalizedMenuName)
                    .food(food)
                    .confidence(parseConfidence(text(row, firstColumn(columns, "confidence"))))
                    .defaultServingGram(number(row, firstColumn(columns, "default_serving_gram", "serving_gram")))
                    .note(maxLength(text(row, firstColumn(columns, "note")), 500))
                    .build());
        }
        List<ManualFoodOverride> overrides = new ArrayList<>(overridesByNormalizedName.values());
        manualFoodOverrideRepository.saveAll(overrides);
        return overrides.size();
    }

    private int importServingDefaults(Workbook workbook) {
        Sheet sheet = workbook.getSheet("serving_defaults");
        if (sheet == null) return 0;
        Map<String, Integer> columns = readHeader(sheet, "serving_defaults");
        Map<String, ServingDefault> defaultsByCategory = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String category = text(row, firstColumn(columns, "category", "display_category", "menu_name"));
            Double gram = number(row, firstColumn(columns, "serving_gram", "default_serving_gram"));
            if (category == null || gram == null) continue;
            String normalizedCategory = maxLength(category, 100);
            defaultsByCategory.putIfAbsent(normalizedCategory, ServingDefault.builder().category(normalizedCategory).servingGram(gram).build());
        }
        List<ServingDefault> defaults = new ArrayList<>(defaultsByCategory.values());
        servingDefaultRepository.saveAll(defaults);
        return defaults.size();
    }

    private MatchConfidence parseConfidence(String value) {
        if (value == null) return MatchConfidence.HIGH;
        try {
            return MatchConfidence.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MatchConfidence.HIGH;
        }
    }

    private Integer firstColumn(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer index = columns.get(name);
            if (index != null) return index;
        }
        return null;
    }

    private Sheet getRequiredSheet(Workbook workbook, String... sheetNames) {
        for (String sheetName : sheetNames) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) return sheet;
        }
        throw new IllegalArgumentException("xlsx에 필수 시트가 없습니다: " + String.join(" 또는 ", sheetNames));
    }

    private Map<String, Integer> readHeader(Sheet sheet, String sheetName) {
        Row header = sheet.getRow(0);
        if (header == null) throw new IllegalArgumentException(sheetName + " 시트의 헤더 행이 비어 있습니다.");
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = text(cell);
            if (name != null) columns.put(name.trim(), cell.getColumnIndex());
        }
        return columns;
    }

    private String text(Row row, Integer columnIndex) {
        return columnIndex == null ? null : text(row.getCell(columnIndex));
    }

    private String text(Cell cell) {
        if (cell == null) return null;
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue().toLocalDate().toString() : formatNumber(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> formulaText(cell);
            case BLANK, ERROR, _NONE -> null;
        };
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() || "-".equals(trimmed) ? null : trimmed;
    }

    private String formulaText(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumber(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case BLANK, ERROR, FORMULA, _NONE -> null;
        };
    }

    private Double number(Row row, Integer columnIndex) {
        if (columnIndex == null) return null;
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String value = text(cell);
        if (value == null) return null;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("숫자 컬럼을 변환할 수 없습니다. row=" + (row.getRowNum() + 1)
                    + ", column=" + (columnIndex + 1) + ", value=" + value, e);
        }
    }

    private Integer integer(Row row, Integer columnIndex) {
        Double value = number(row, columnIndex);
        return value == null ? null : value.intValue();
    }

    private String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    private Map<String, Food> toFoodByNormalizedName(List<Food> foods) {
        Map<String, Food> foodByName = new LinkedHashMap<>();
        for (Food food : foods) foodByName.putIfAbsent(dedupeKey(food.getName()), food);
        return foodByName;
    }

    private boolean hasGreaterSourceCount(Food candidate, Food current) {
        return Optional.ofNullable(candidate.getSourceCount()).orElse(0) > Optional.ofNullable(current.getSourceCount()).orElse(0);
    }

    private boolean isInvalidFoodName(String name) {
        if (name == null) return true;
        String normalized = foodNameNormalizer.toSearchName(name).toLowerCase(Locale.ROOT);
        if (normalized.length() < 2) return true;
        if (normalized.matches("^\\d+(\\.\\d+)?$")) return true;
        if (normalized.matches("^\\d+(\\.\\d+)?(g|kg|mg|ml|l|개|봉|팩|회|인분)$")) return true;
        return !normalized.matches(".*[a-z가-힣].*");
    }

    private String dedupeKey(String value) {
        return value == null ? "" : foodNameNormalizer.toSearchName(value).toLowerCase(Locale.ROOT);
    }

    private String maxLength(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void detachUserMealFoodReferencesIfPresent() {
        if (!hasColumn("user_meal_foods", "food_id")) return;
        entityManager.createNativeQuery("update user_meal_foods set food_id = null where food_id is not null").executeUpdate();
        entityManager.flush();
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String tableCandidate : tableNameCandidates(tableName)) {
                try (ResultSet tables = metaData.getTables(catalog, null, tableCandidate, new String[]{"TABLE"})) {
                    if (!tables.next()) continue;
                }
                for (String columnCandidate : tableNameCandidates(columnName)) {
                    try (ResultSet columns = metaData.getColumns(catalog, null, tableCandidate, columnCandidate)) {
                        if (columns.next()) return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("식품 데이터 import를 위한 DB 메타데이터 확인 중 오류가 발생했습니다.", e);
        }
    }

    private List<String> tableNameCandidates(String value) {
        return List.of(value, value.toUpperCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
    }

    private record FoodReadResult(List<Food> foods, int skippedCount) {}
    private record AliasReadResult(List<FoodAlias> aliases, int skippedCount) {}
}
