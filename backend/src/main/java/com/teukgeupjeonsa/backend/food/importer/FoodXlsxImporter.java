package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class FoodXlsxImporter {

    private static final String FOOD_MASTER_SHEET = "food_master";
    private static final String FOOD_ALIAS_SHEET = "food_alias";

    private static final Map<String, String> FOOD_MASTER_COLUMNS = Map.ofEntries(
            Map.entry("name", "food_name"),
            Map.entry("category", "display_category"),
            Map.entry("servingUnit", "basis"),
            Map.entry("calorie", "calorie_kcal"),
            Map.entry("carbohydrate", "carbohydrate_g"),
            Map.entry("protein", "protein_g"),
            Map.entry("fat", "fat_g"),
            Map.entry("sugar", "sugar_g"),
            Map.entry("sodium", "sodium_mg"),
            Map.entry("cholesterol", "cholesterol_mg"),
            Map.entry("saturatedFat", "saturated_fat_g"),
            Map.entry("transFat", "trans_fat_g"),
            Map.entry("sourceCount", "source_count")
    );

    private static final Map<String, String> FOOD_ALIAS_COLUMNS = Map.ofEntries(
            Map.entry("aliasName", "raw_food_name"),
            Map.entry("makerName", "maker_name"),
            Map.entry("category", "display_category"),
            Map.entry("foodName", "final_food_name")
    );

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final EntityManager entityManager;
    private final DataSource dataSource;

    @Transactional
    public FoodImportResult importXlsx(Path xlsxPath) {
        if (!Files.exists(xlsxPath)) {
            throw new IllegalArgumentException("식품 xlsx 파일을 찾을 수 없습니다: " + xlsxPath.toAbsolutePath());
        }

        try (InputStream inputStream = Files.newInputStream(xlsxPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            List<Food> foods = readFoods(workbook);

            foodAliasRepository.deleteAllInBatch();
            detachUserMealFoodReferencesIfPresent();
            foodRepository.deleteAllInBatch();
            foodRepository.flush();

            List<Food> savedFoods = foodRepository.saveAll(foods);
            foodRepository.flush();

            Map<String, Food> foodByName = savedFoods.stream()
                    .collect(Collectors.toMap(Food::getName, Function.identity(), (first, second) -> first));

            AliasReadResult aliasReadResult = readAliases(workbook, foodByName);
            foodAliasRepository.saveAll(aliasReadResult.aliases());

            return new FoodImportResult(savedFoods.size(), aliasReadResult.aliases().size(), aliasReadResult.skippedCount());
        } catch (IOException e) {
            throw new IllegalStateException("식품 xlsx 파일을 읽는 중 오류가 발생했습니다: " + xlsxPath.toAbsolutePath(), e);
        }
    }

    private List<Food> readFoods(Workbook workbook) {
        Sheet sheet = getRequiredSheet(workbook, FOOD_MASTER_SHEET);
        Map<String, Integer> columns = readHeader(sheet, FOOD_MASTER_SHEET);
        requireColumns(columns, FOOD_MASTER_COLUMNS.values(), FOOD_MASTER_SHEET);

        Map<String, Food> foodsByName = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String name = maxLength(text(row, columns.get(FOOD_MASTER_COLUMNS.get("name"))), 200);
            if (name == null) {
                continue;
            }

            Food food = Food.builder()
                    .name(name)
                    .searchName(maxLength(normalizeSearchName(name), 200))
                    .category(maxLength(text(row, columns.get(FOOD_MASTER_COLUMNS.get("category"))), 80))
                    .servingUnit(maxLength(Optional.ofNullable(text(row, columns.get(FOOD_MASTER_COLUMNS.get("servingUnit")))).orElse("100g"), 20))
                    .calorie(number(row, columns.get(FOOD_MASTER_COLUMNS.get("calorie"))))
                    .carbohydrate(number(row, columns.get(FOOD_MASTER_COLUMNS.get("carbohydrate"))))
                    .protein(number(row, columns.get(FOOD_MASTER_COLUMNS.get("protein"))))
                    .fat(number(row, columns.get(FOOD_MASTER_COLUMNS.get("fat"))))
                    .sugar(number(row, columns.get(FOOD_MASTER_COLUMNS.get("sugar"))))
                    .sodium(number(row, columns.get(FOOD_MASTER_COLUMNS.get("sodium"))))
                    .cholesterol(number(row, columns.get(FOOD_MASTER_COLUMNS.get("cholesterol"))))
                    .saturatedFat(number(row, columns.get(FOOD_MASTER_COLUMNS.get("saturatedFat"))))
                    .transFat(number(row, columns.get(FOOD_MASTER_COLUMNS.get("transFat"))))
                    .sourceCount(integer(row, columns.get(FOOD_MASTER_COLUMNS.get("sourceCount"))))
                    .build();
            foodsByName.put(name, food);
        }

        return new ArrayList<>(foodsByName.values());
    }

    private AliasReadResult readAliases(Workbook workbook, Map<String, Food> foodByName) {
        Sheet sheet = getRequiredSheet(workbook, FOOD_ALIAS_SHEET);
        Map<String, Integer> columns = readHeader(sheet, FOOD_ALIAS_SHEET);
        requireColumns(columns, FOOD_ALIAS_COLUMNS.values(), FOOD_ALIAS_SHEET);

        List<FoodAlias> aliases = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skippedCount = 0;

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String aliasName = maxLength(text(row, columns.get(FOOD_ALIAS_COLUMNS.get("aliasName"))), 300);
            if (aliasName == null) {
                skippedCount++;
                continue;
            }

            String mappedFoodName = maxLength(text(row, columns.get(FOOD_ALIAS_COLUMNS.get("foodName"))), 200);
            if (mappedFoodName == null) {
                mappedFoodName = aliasName;
            }

            Food food = foodByName.get(mappedFoodName);
            if (food == null) {
                skippedCount++;
                continue;
            }

            String originalName = aliasName;
            String makerName = text(row, columns.get(FOOD_ALIAS_COLUMNS.get("makerName")));
            if (makerName != null) {
                originalName = maxLength(aliasName + "(" + makerName + ")", 300);
            }

            String key = food.getId() + "\n" + aliasName + "\n" + originalName;
            if (!seen.add(key)) {
                continue;
            }

            aliases.add(FoodAlias.builder()
                    .food(food)
                    .aliasName(aliasName)
                    .searchName(maxLength(normalizeSearchName(aliasName), 300))
                    .originalName(originalName)
                    .category(maxLength(text(row, columns.get(FOOD_ALIAS_COLUMNS.get("category"))), 80))
                    .build());
        }

        return new AliasReadResult(aliases, skippedCount);
    }

    private void detachUserMealFoodReferencesIfPresent() {
        if (!hasColumn("user_meal_foods", "food_id")) {
            return;
        }
        entityManager.createNativeQuery("update user_meal_foods set food_id = null where food_id is not null")
                .executeUpdate();
        entityManager.flush();
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String tableCandidate : tableNameCandidates(tableName)) {
                try (ResultSet tables = metaData.getTables(catalog, null, tableCandidate, new String[]{"TABLE"})) {
                    if (!tables.next()) {
                        continue;
                    }
                }

                for (String columnCandidate : tableNameCandidates(columnName)) {
                    try (ResultSet columns = metaData.getColumns(catalog, null, tableCandidate, columnCandidate)) {
                        if (columns.next()) {
                            return true;
                        }
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

    private Sheet getRequiredSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("xlsx에 필수 시트가 없습니다: " + sheetName);
        }
        return sheet;
    }

    private Map<String, Integer> readHeader(Sheet sheet, String sheetName) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException(sheetName + " 시트의 헤더 행이 비어 있습니다.");
        }

        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = text(cell);
            if (name != null) {
                columns.put(name, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private void requireColumns(Map<String, Integer> columns, Collection<String> requiredColumns, String sheetName) {
        List<String> missing = requiredColumns.stream()
                .filter(column -> !columns.containsKey(column))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(sheetName + " 시트에 필수 컬럼이 없습니다: " + missing);
        }
    }

    private String text(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        return text(row.getCell(columnIndex));
    }

    private String text(Cell cell) {
        if (cell == null) {
            return null;
        }

        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : formatNumber(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> formulaText(cell);
            case BLANK, ERROR, _NONE -> null;
        };

        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed)) {
            return null;
        }
        return trimmed;
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
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        String value = text(cell);
        if (value == null) {
            return null;
        }
        String normalized = value.replace(",", "");
        try {
            return Double.parseDouble(normalized);
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
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private String normalizeSearchName(String value) {
        return value.replaceAll("\\s+", "");
    }

    private String maxLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record AliasReadResult(List<FoodAlias> aliases, int skippedCount) {
    }
}
