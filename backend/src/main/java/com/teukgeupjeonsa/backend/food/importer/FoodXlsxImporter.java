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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

            List<FoodRow> foodRows = foodReadResult.rows();
            List<Food> savedFoods = foodRepository.saveAll(foodRows.stream().map(FoodRow::food).toList());
            foodRepository.flush();
            validateNutritionColumns(savedFoods);

            FoodLookup foodLookup = buildFoodLookup(foodRows, foodReadResult.externalFoodIdToDedupeKey());

            AliasReadResult aliasReadResult = readAliases(workbook, foodLookup);
            foodAliasRepository.saveAll(aliasReadResult.aliases());

            int servingDefaultCount = importServingDefaults(workbook);
            int overrideCount = importManualOverrides(workbook, foodLookup);

            return new FoodImportResult(savedFoods.size(), aliasReadResult.aliases().size(),
                    foodReadResult.skippedCount(), aliasReadResult.skippedCount(), overrideCount, servingDefaultCount);
        } catch (IOException e) {
            throw new IllegalStateException("식품 xlsx 파일을 읽는 중 오류가 발생했습니다: " + xlsxPath.toAbsolutePath(), e);
        }
    }

    private FoodReadResult readFoods(Workbook workbook) {
        Sheet sheet = getRequiredSheet(workbook, "food_master_clean_100g", "food_master");
        Map<String, Integer> columns = readHeader(sheet, sheet.getSheetName());

        Integer foodIdCol = firstColumn(columns, "food_id");
        Integer nameCol = requiredColumn(columns, sheet.getSheetName(), "representative_name", "representative_name", "food_name", "name", "final_food_name");
        Integer categoryCol = requiredColumn(columns, sheet.getSheetName(), "display_category", "display_category", "category");
        Integer kcalCol = requiredColumn(columns, sheet.getSheetName(), "kcal_100g", "kcal_100g", "kcal_per_100g", "calorie_kcal", "calorie", "calories", "kcal");
        Integer carbCol = requiredColumn(columns, sheet.getSheetName(), "carb_100g", "carb_100g", "carbohydrate_100g", "carbohydrate_per_100g", "carbohydrate_g", "carb_g");
        Integer proteinCol = requiredColumn(columns, sheet.getSheetName(), "protein_100g", "protein_100g", "protein_per_100g", "protein_g");
        Integer fatCol = requiredColumn(columns, sheet.getSheetName(), "fat_100g", "fat_100g", "fat_per_100g", "fat_g");
        Integer matchKeyCol = firstColumn(columns, "match_key", "search_name", "normalized_name");
        Integer sourceCountCol = firstColumn(columns, "source_count", "alias_count");
        Integer qualityFlagCol = firstColumn(columns, "quality_flag");

        Map<String, FoodRow> rowsByNameKey = new LinkedHashMap<>();
        Map<String, String> externalFoodIdToDedupeKey = new LinkedHashMap<>();
        int skippedCount = 0;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String externalFoodId = text(row, foodIdCol);
            if (externalFoodId == null || externalFoodId.isBlank()) {
                externalFoodId = String.valueOf(rowIndex);
            }
            String name = maxLength(text(row, nameCol), 200);
            if (isInvalidFoodName(name)) {
                skippedCount++;
                continue;
            }

            String category = maxLength(text(row, categoryCol), 80);
            String matchKey = text(row, matchKeyCol);
            String searchName = matchKey == null || matchKey.isBlank()
                    ? foodNameNormalizer.toSearchName(name)
                    : foodNameNormalizer.toSearchName(matchKey);

            Food food = Food.builder()
                    .name(name)
                    .searchName(maxLength(searchName, 200))
                    .category(category)
                    .servingUnit("100g")
                    .calorie(number(row, kcalCol))
                    .carbohydrate(number(row, carbCol))
                    .protein(number(row, proteinCol))
                    .fat(number(row, fatCol))
                    .sugar(number(row, firstColumn(columns, "sugar_g")))
                    .sodium(number(row, firstColumn(columns, "sodium_mg")))
                    .cholesterol(number(row, firstColumn(columns, "cholesterol_mg")))
                    .saturatedFat(number(row, firstColumn(columns, "saturated_fat_g")))
                    .transFat(number(row, firstColumn(columns, "trans_fat_g")))
                    .sourceCount(integer(row, sourceCountCol))
                    .qualityFlag(maxLength(text(row, qualityFlagCol), 40))
                    .build();

            String dedupeKey = dedupeKey(name);
            if (externalFoodId != null && !externalFoodId.isBlank()) {
                externalFoodIdToDedupeKey.put(externalFoodId.trim(), dedupeKey);
            }

            FoodRow candidate = new FoodRow(externalFoodId == null ? null : externalFoodId.trim(), name, dedupeKey, food);
            FoodRow previous = rowsByNameKey.get(dedupeKey);
            if (previous == null || hasGreaterSourceCount(food, previous.food())) {
                rowsByNameKey.put(dedupeKey, candidate);
            }
            if (previous != null) skippedCount++;
        }
        return new FoodReadResult(new ArrayList<>(rowsByNameKey.values()), externalFoodIdToDedupeKey, skippedCount);
    }

    private FoodLookup buildFoodLookup(List<FoodRow> foodRows, Map<String, String> externalFoodIdToDedupeKey) {
        Map<String, Food> foodByName = new LinkedHashMap<>();
        for (FoodRow row : foodRows) {
            Food food = row.food();
            foodByName.putIfAbsent(row.dedupeKey(), food);
            foodByName.putIfAbsent(dedupeKey(row.representativeName()), food);
            foodByName.putIfAbsent(dedupeKey(food.getName()), food);
        }

        Map<String, Food> foodByExternalId = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : externalFoodIdToDedupeKey.entrySet()) {
            Food food = foodByName.get(entry.getValue());
            if (food != null) {
                foodByExternalId.put(entry.getKey(), food);
            }
        }
        return new FoodLookup(foodByExternalId, foodByName);
    }

    private void validateNutritionColumns(List<Food> savedFoods) {
        long calorieNotNull = savedFoods.stream().filter(food -> food.getCalorie() != null).count();
        long proteinNotNull = savedFoods.stream().filter(food -> food.getProtein() != null).count();

        if (!savedFoods.isEmpty() && calorieNotNull == 0) {
            throw new IllegalStateException("food_master_clean_100g의 kcal_100g 컬럼을 읽지 못했습니다. foods.calorie가 전부 NULL입니다.");
        }
        if (!savedFoods.isEmpty() && proteinNotNull == 0) {
            throw new IllegalStateException("food_master_clean_100g의 protein_100g 컬럼을 읽지 못했습니다. foods.protein이 전부 NULL입니다.");
        }

        System.out.println("foods calorie not null: " + calorieNotNull + " / " + savedFoods.size());
        System.out.println("foods protein not null: " + proteinNotNull + " / " + savedFoods.size());
    }

    private AliasReadResult readAliases(Workbook workbook, FoodLookup foodLookup) {
        Sheet sheet = workbook.getSheet("food_alias");
        if (sheet == null) return new AliasReadResult(List.of(), 0);
        Map<String, Integer> columns = readHeader(sheet, "food_alias");

        Integer aliasNameCol = requiredColumn(columns, "food_alias", "raw_food_name", "raw_food_name", "alias_name", "original_name");
        Integer foodIdCol = firstColumn(columns, "food_id");
        Integer representativeNameCol = firstColumn(columns, "representative_name", "matched_food_name", "food_name", "final_food_name");
        Integer normalizedRawNameCol = firstColumn(columns, "normalized_raw_name", "search_name");
        Integer categoryCol = firstColumn(columns, "display_category", "category", "raw_group");

        List<FoodAlias> aliases = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skippedCount = 0;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String aliasName = maxLength(text(row, aliasNameCol), 300);
            if (aliasName == null || aliasName.isBlank()) {
                skippedCount++;
                continue;
            }

            String representativeName = text(row, representativeNameCol);
            Food food = findFood(foodLookup, text(row, foodIdCol), representativeName);
            if (food == null) {
                food = findFood(foodLookup, null, aliasName);
            }
            if (food == null) {
                skippedCount++;
                continue;
            }

            String normalizedRawName = text(row, normalizedRawNameCol);
            String searchName = normalizedRawName == null || normalizedRawName.isBlank()
                    ? foodNameNormalizer.toSearchName(aliasName)
                    : foodNameNormalizer.toSearchName(normalizedRawName);
            String originalName = aliasName;
            String key = food.getId() + "\n" + dedupeKey(aliasName) + "\n" + dedupeKey(originalName);
            if (!seen.add(key)) continue;

            aliases.add(FoodAlias.builder()
                    .food(food)
                    .aliasName(aliasName)
                    .searchName(maxLength(searchName, 300))
                    .originalName(maxLength(originalName, 300))
                    .category(maxLength(text(row, categoryCol), 80))
                    .build());
        }
        return new AliasReadResult(aliases, skippedCount);
    }

    private int importManualOverrides(Workbook workbook, FoodLookup foodLookup) {
        Sheet sheet = workbook.getSheet("manual_overrides");
        if (sheet == null) return 0;
        Map<String, Integer> columns = readHeader(sheet, "manual_overrides");

        Integer rawMenuCol = requiredColumn(columns, "manual_overrides", "raw_menu_name", "raw_menu_name", "menu_name");
        Integer normalizedMenuCol = firstColumn(columns, "normalized_menu_name", "search_name");
        Integer foodIdCol = firstColumn(columns, "food_id");
        Integer matchedFoodNameCol = firstColumn(columns, "matched_food_name", "representative_name", "food_name", "final_food_name");
        Integer servingCol = firstColumn(columns, "serving_g", "default_serving_gram", "serving_gram");
        Integer confidenceCol = firstColumn(columns, "confidence");
        Integer ruleCol = firstColumn(columns, "rule");
        Integer noteCol = firstColumn(columns, "note");

        Map<String, ManualFoodOverride> overridesByNormalizedName = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String rawMenu = text(row, rawMenuCol);
            if (rawMenu == null || rawMenu.isBlank()) continue;

            Food food = findFood(foodLookup, text(row, foodIdCol), text(row, matchedFoodNameCol));
            if (food == null) continue;

            String normalizedFromSheet = text(row, normalizedMenuCol);
            String normalizedMenuName = normalizedFromSheet == null || normalizedFromSheet.isBlank()
                    ? foodNameNormalizer.toSearchName(rawMenu)
                    : foodNameNormalizer.toSearchName(normalizedFromSheet);
            String mergedNote = Stream.of(text(row, ruleCol), text(row, noteCol))
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" / "));

            overridesByNormalizedName.putIfAbsent(normalizedMenuName, ManualFoodOverride.builder()
                    .rawMenuName(maxLength(rawMenu, 200))
                    .normalizedMenuName(maxLength(normalizedMenuName, 200))
                    .food(food)
                    .confidence(parseConfidence(text(row, confidenceCol)))
                    .defaultServingGram(number(row, servingCol))
                    .note(maxLength(mergedNote.isBlank() ? null : mergedNote, 500))
                    .build());
        }
        List<ManualFoodOverride> overrides = new ArrayList<>(overridesByNormalizedName.values());
        manualFoodOverrideRepository.saveAll(overrides);
        return overrides.size();
    }

    private Food findFood(FoodLookup foodLookup, String externalFoodId, String representativeName) {
        if (externalFoodId != null && !externalFoodId.isBlank()) {
            Food food = foodLookup.foodByExternalId().get(externalFoodId.trim());
            if (food != null) return food;
        }
        if (representativeName != null && !representativeName.isBlank()) {
            return foodLookup.foodByName().get(dedupeKey(representativeName));
        }
        return null;
    }

    private int importServingDefaults(Workbook workbook) {
        Sheet sheet = workbook.getSheet("serving_defaults");
        if (sheet == null) return 0;
        Map<String, Integer> columns = readHeader(sheet, "serving_defaults");

        Integer categoryCol = requiredColumn(columns, "serving_defaults", "display_category", "display_category", "category");
        Integer gramCol = requiredColumn(columns, "serving_defaults", "default_serving_g", "default_serving_g", "serving_g", "default_serving_gram", "serving_gram");

        Map<String, ServingDefault> defaultsByCategory = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String category = text(row, categoryCol);
            Double gram = number(row, gramCol);
            if (category == null || category.isBlank() || gram == null) continue;
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

    private String canonicalHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Integer firstColumn(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer index = columns.get(canonicalHeader(name));
            if (index != null) return index;
        }
        return null;
    }

    private Integer requiredColumn(Map<String, Integer> columns, String sheetName, String logicalName, String... names) {
        Integer index = firstColumn(columns, names);
        if (index == null) {
            throw new IllegalArgumentException(sheetName + " 시트에 필수 컬럼이 없습니다: " + logicalName
                    + ", candidates=" + String.join(", ", names)
                    + ", actual=" + columns.keySet());
        }
        return index;
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
            if (name != null) columns.put(canonicalHeader(name), cell.getColumnIndex());
        }
        System.out.println(sheetName + " columns = " + columns.keySet());
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

    private record FoodRow(String externalFoodId, String representativeName, String dedupeKey, Food food) {}
    private record FoodLookup(Map<String, Food> foodByExternalId, Map<String, Food> foodByName) {}
    private record FoodReadResult(List<FoodRow> rows, Map<String, String> externalFoodIdToDedupeKey, int skippedCount) {}
    private record AliasReadResult(List<FoodAlias> aliases, int skippedCount) {}
}
