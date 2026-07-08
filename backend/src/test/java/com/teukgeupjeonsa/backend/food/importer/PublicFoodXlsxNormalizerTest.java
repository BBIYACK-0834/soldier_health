package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PublicFoodXlsxNormalizerTest {
    @TempDir Path tempDir;

    @Test
    void normalizesPublicFoodXlsxToImporterSheetsWithMedianPer100g() throws Exception {
        Path source = tempDir.resolve("public.xlsx");
        Path target = tempDir.resolve("clean.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("raw");
            Row description = sheet.createRow(0);
            description.createCell(0).setCellValue("설명 행");
            Row header = sheet.createRow(1);
            String[] headers = {"DESC_KOR", "GROUP_NAME", "SERVING_SIZE", "NUTR_CONT1", "NUTR_CONT2", "NUTR_CONT3", "NUTR_CONT4"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            row(sheet, 2, "닭고기", "육류", 50, 100, 2, 10, 1);
            row(sheet, 3, "닭고기", "육류", 100, 210, 6, 22, 3);
            row(sheet, 4, "빈값", "기타", 0, 10, 1, 1, 1);
            try (OutputStream out = Files.newOutputStream(source)) { workbook.write(out); }
        }

        PublicFoodXlsxNormalizer.NormalizeResult result = new PublicFoodXlsxNormalizer(new FoodNameNormalizer()).normalize(source, target);

        assertThat(result.getFoods()).isEqualTo(1);
        assertThat(result.getSkippedRows()).isEqualTo(1);
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(target))) {
            Sheet foodMaster = workbook.getSheet("food_master_clean_100g");
            assertThat(foodMaster).isNotNull();
            Map<String, Integer> columns = columns(foodMaster.getRow(0));
            Row food = foodMaster.getRow(1);
            assertThat(food.getCell(columns.get("representative_name")).getStringCellValue()).isEqualTo("닭고기");
            assertThat(food.getCell(columns.get("kcal_100g")).getNumericCellValue()).isEqualTo(205.0);
            assertThat(food.getCell(columns.get("source_count")).getNumericCellValue()).isEqualTo(2.0);
            assertThat(workbook.getSheet("food_alias")).isNotNull();
            assertThat(workbook.getSheet("serving_defaults")).isNotNull();
            assertThat(workbook.getSheet("review_needed")).isNotNull();
        }
    }

    @Test
    void supportsUserFriendlyFoodMasterColumnNames() throws Exception {
        Path source = tempDir.resolve("friendly.xlsx");
        Path target = tempDir.resolve("friendly-clean.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("food_master");
            Row header = sheet.createRow(0);
            String[] headers = {"food_name", "display_category", "basis", "calorie_kcal", "carbohydrate_g", "protein_g", "fat_g", "source_count", "source_name_samples"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("감자칩");
            row.createCell(1).setCellValue("간식");
            row.createCell(2).setCellValue("100g");
            row.createCell(3).setCellValue(249.22);
            row.createCell(4).setCellValue(19.77);
            row.createCell(5).setCellValue(1.83);
            row.createCell(6).setCellValue(11.08);
            row.createCell(7).setCellValue(34);
            row.createCell(8).setCellValue("포카칩 오리지널 | 프링글스 양파맛");
            try (OutputStream out = Files.newOutputStream(source)) { workbook.write(out); }
        }

        new PublicFoodXlsxNormalizer(new FoodNameNormalizer()).normalize(source, target);

        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(target))) {
            Sheet foodMaster = workbook.getSheet("food_master_clean_100g");
            Map<String, Integer> columns = columns(foodMaster.getRow(0));
            assertThat(foodMaster.getRow(1).getCell(columns.get("representative_name")).getStringCellValue()).isEqualTo("감자칩");
            assertThat(foodMaster.getRow(1).getCell(columns.get("kcal_100g")).getNumericCellValue()).isEqualTo(249.22);
            assertThat(workbook.getSheet("food_alias").getLastRowNum()).isGreaterThanOrEqualTo(2);
        }
    }

    private void row(Sheet sheet, int rowIndex, String name, String group, double serving, double kcal, double carb, double protein, double fat) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(group);
        row.createCell(2).setCellValue(serving);
        row.createCell(3).setCellValue(kcal);
        row.createCell(4).setCellValue(carb);
        row.createCell(5).setCellValue(protein);
        row.createCell(6).setCellValue(fat);
    }

    private Map<String, Integer> columns(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : row) columns.put(cell.getStringCellValue(), cell.getColumnIndex());
        return columns;
    }
}
