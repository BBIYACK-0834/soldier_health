package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PublicFoodXlsxNormalizer {
    private static final double OUTLIER_SERVING_SIZE_GRAM = 2000.0;
    private final FoodNameNormalizer foodNameNormalizer;

    public NormalizeResult normalize(Path sourceXlsx, Path outputXlsx) {
        if (!Files.exists(sourceXlsx)) throw new IllegalArgumentException("원본 xlsx 파일을 찾을 수 없습니다: " + sourceXlsx.toAbsolutePath());
        try (InputStream input = Files.newInputStream(sourceXlsx);
             Workbook source = WorkbookFactory.create(input);
             Workbook output = new XSSFWorkbook()) {
            Sheet sheet = source.getSheetAt(0);
            Map<String, Integer> columns = readHeader(sheet);
            Map<String, Group> groups = new LinkedHashMap<>();
            int skipped = 0;
            int outliers = 0;
            for (int i = findHeaderRowIndex(sheet) + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = text(row, first(columns, "DESC_KOR", "food_name", "final_food_name"));
                String category = text(row, first(columns, "GROUP_NAME", "display_category", "main_original_group", "original_group"));
                Double serving = number(row, first(columns, "SERVING_SIZE", "serving_size"));
                if (serving == null && "100g".equalsIgnoreCase(Optional.ofNullable(text(row, first(columns, "basis"))).orElse("").trim())) {
                    serving = 100.0;
                }
                if (name == null || name.isBlank() || serving == null || serving <= 0) { skipped++; continue; }
                boolean outlier = serving > OUTLIER_SERVING_SIZE_GRAM;
                if (outlier) outliers++;
                String key = foodNameNormalizer.toSearchName(name);
                if (key.isBlank()) { skipped++; continue; }
                groups.computeIfAbsent(key, ignored -> new Group(name, category, key))
                        .add(new SourceRow(name, category, serving, outlier,
                                per100(row, first(columns, "NUTR_CONT1", "calorie_kcal", "kcal_100g"), serving),
                                per100(row, first(columns, "NUTR_CONT2", "carbohydrate_g", "carb_100g"), serving),
                                per100(row, first(columns, "NUTR_CONT3", "protein_g", "protein_100g"), serving),
                                per100(row, first(columns, "NUTR_CONT4", "fat_g", "fat_100g"), serving),
                                per100(row, first(columns, "NUTR_CONT5", "sugar_g"), serving),
                                per100(row, first(columns, "NUTR_CONT6", "sodium_mg"), serving),
                                per100(row, first(columns, "NUTR_CONT7", "cholesterol_mg"), serving),
                                per100(row, first(columns, "NUTR_CONT8", "saturated_fat_g"), serving),
                                per100(row, first(columns, "NUTR_CONT9", "trans_fat_g"), serving)),
                                text(row, first(columns, "source_name_samples", "raw_food_name")));
            }
            writeFoodMaster(output, groups.values());
            writeAliases(output, groups.values());
            writeServingDefaults(output);
            writeReviewNeeded(output, groups.values());
            try (OutputStream out = Files.newOutputStream(outputXlsx)) { output.write(out); }
            return NormalizeResult.builder().sourceRows(sheet.getLastRowNum()).foods(groups.size()).aliases(groups.values().stream().mapToInt(g -> g.aliases.size()).sum()).skippedRows(skipped).outlierRows(outliers).outputPath(outputXlsx.toAbsolutePath().toString()).build();
        } catch (IOException e) {
            throw new IllegalStateException("xlsx 정제 중 오류가 발생했습니다.", e);
        }
    }

    private void writeFoodMaster(Workbook workbook, Collection<Group> groups) {
        Sheet sheet = workbook.createSheet("food_master_clean_100g");
        writeHeader(sheet, "food_id", "representative_name", "display_category", "match_key", "kcal_100g", "carb_100g", "protein_100g", "fat_100g", "sugar_g", "sodium_mg", "cholesterol_mg", "saturated_fat_g", "trans_fat_g", "source_count", "quality_flag");
        int rowIndex = 1;
        int foodId = 1;
        for (Group group : groups) {
            Row row = sheet.createRow(rowIndex++);
            int c = 0;
            row.createCell(c++).setCellValue(foodId++);
            row.createCell(c++).setCellValue(group.representativeName);
            row.createCell(c++).setCellValue(group.category == null ? "" : group.category);
            row.createCell(c++).setCellValue(group.key);
            for (int nutrient = 0; nutrient < 9; nutrient++) row.createCell(c++).setCellValue(group.median(nutrient));
            row.createCell(c++).setCellValue(group.rows.size());
            row.createCell(c).setCellValue(group.hasOutlier() ? "OUTLIER" : "OK");
        }
    }

    private void writeAliases(Workbook workbook, Collection<Group> groups) {
        Sheet sheet = workbook.createSheet("food_alias");
        writeHeader(sheet, "food_id", "alias_name", "original_name", "display_category");
        int rowIndex = 1;
        int foodId = 1;
        for (Group group : groups) {
            for (String alias : group.aliases) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(foodId);
                row.createCell(1).setCellValue(alias);
                row.createCell(2).setCellValue(alias);
                row.createCell(3).setCellValue(group.category == null ? "" : group.category);
            }
            foodId++;
        }
    }

    private void writeServingDefaults(Workbook workbook) {
        Sheet sheet = workbook.createSheet("serving_defaults");
        writeHeader(sheet, "category", "serving_gram");
        Object[][] rows = {{"밥류", 210}, {"국/탕/찌개류", 300}, {"볶음류", 160}, {"조림류", 150}, {"구이류", 140}, {"튀김류", 130}, {"무침류", 80}, {"김치류", 40}};
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue((String) rows[i][0]);
            row.createCell(1).setCellValue((Integer) rows[i][1]);
        }
    }

    private void writeReviewNeeded(Workbook workbook, Collection<Group> groups) {
        Sheet sheet = workbook.createSheet("review_needed");
        writeHeader(sheet, "representative_name", "reason", "source_count");
        int rowIndex = 1;
        for (Group group : groups.stream().filter(Group::hasOutlier).toList()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(group.representativeName);
            row.createCell(1).setCellValue("SERVING_SIZE_OUTLIER_INCLUDED_AS_FLAG");
            row.createCell(2).setCellValue(group.rows.size());
        }
    }

    private Map<String, Integer> readHeader(Sheet sheet) {
        for (int r = 0; r <= Math.min(3, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : row) columns.put(cell.toString().trim(), cell.getColumnIndex());
            if ((columns.containsKey("DESC_KOR") && columns.containsKey("SERVING_SIZE")) || columns.containsKey("food_name")) return columns;
        }
        throw new IllegalArgumentException("DESC_KOR/SERVING_SIZE 헤더를 찾을 수 없습니다.");
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int r = 0; r <= Math.min(3, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Set<String> headers = new HashSet<>();
            for (Cell cell : row) headers.add(cell.toString().trim());
            if ((headers.contains("DESC_KOR") && headers.contains("SERVING_SIZE")) || headers.contains("food_name")) return r;
        }
        throw new IllegalArgumentException("식품 데이터 헤더 행을 찾을 수 없습니다.");
    }

    private Integer first(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer column = columns.get(name);
            if (column != null) return column;
        }
        return null;
    }

    private void writeHeader(Sheet sheet, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
    }
    private String text(Row row, Integer column) { if (column == null) return null; Cell cell = row.getCell(column); if (cell == null) return null; return switch (cell.getCellType()) { case STRING -> cell.getStringCellValue().trim(); case NUMERIC -> String.valueOf(cell.getNumericCellValue()); default -> cell.toString().trim(); }; }
    private Double number(Row row, Integer column) { if (column == null) return null; Cell cell = row.getCell(column); if (cell == null) return null; try { return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : Double.parseDouble(cell.toString().trim()); } catch (NumberFormatException e) { return null; } }
    private Double per100(Row row, Integer column, double serving) { Double value = number(row, column); return value == null ? null : value / serving * 100.0; }

    @Getter @Builder public static class NormalizeResult { private int sourceRows; private int foods; private int aliases; private int skippedRows; private int outlierRows; private String outputPath; }
    private record SourceRow(String name, String category, double serving, boolean outlier, Double kcal, Double carb, Double protein, Double fat, Double sugar, Double sodium, Double cholesterol, Double saturatedFat, Double transFat) { Double nutrient(int i){ return switch(i){ case 0 -> kcal; case 1 -> carb; case 2 -> protein; case 3 -> fat; case 4 -> sugar; case 5 -> sodium; case 6 -> cholesterol; case 7 -> saturatedFat; case 8 -> transFat; default -> null;};}}
    private static class Group { final String representativeName; final String category; final String key; final List<SourceRow> rows = new ArrayList<>(); final Set<String> aliases = new LinkedHashSet<>(); Group(String representativeName, String category, String key){this.representativeName=representativeName;this.category=category;this.key=key;} void add(SourceRow row, String samples){rows.add(row); aliases.add(row.name()); if(samples != null){ Arrays.stream(samples.split("\\|")).map(String::trim).filter(v -> !v.isBlank()).forEach(aliases::add); }} boolean hasOutlier(){return rows.stream().anyMatch(SourceRow::outlier);} double median(int nutrient){ List<Double> values=rows.stream().map(row -> row.nutrient(nutrient)).filter(Objects::nonNull).sorted().toList(); if(values.isEmpty()) return 0; int mid=values.size()/2; return values.size()%2==1 ? values.get(mid) : (values.get(mid-1)+values.get(mid))/2.0; }}
}
