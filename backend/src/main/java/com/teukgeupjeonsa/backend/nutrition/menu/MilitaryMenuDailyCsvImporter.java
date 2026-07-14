package com.teukgeupjeonsa.backend.nutrition.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
public class MilitaryMenuDailyCsvImporter {
    private static final List<String> HEADER = List.of(
            "unit_code", "meal_date", "meal_type", "search_name", "canonical_name", "calorie_kcal", "sample_count");
    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int importGzipCsv(Path path) {
        int validRows = validate(path);
        jdbcTemplate.update("delete from military_menu_daily_profiles");
        String sql = "insert into military_menu_daily_profiles "
                + "(unit_code, meal_date, meal_type, search_name, canonical_name, calorie_kcal, sample_count) "
                + "values (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int inserted = 0;
        try (BufferedReader reader = reader(path)) {
            readCsvRow(reader);
            List<String> row;
            while ((row = readCsvRow(reader)) != null) {
                DailyRow value = parse(row);
                if (value == null) continue;
                batch.add(new Object[]{value.unitCode(), java.sql.Date.valueOf(value.mealDate()), value.mealType(),
                        value.searchName(), value.canonicalName(), value.calorieKcal(), value.sampleCount()});
                if (batch.size() == BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    inserted += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
                inserted += batch.size();
            }
        } catch (IOException e) {
            throw new IllegalStateException("날짜별 군 식단 파일 import 실패: " + path, e);
        }
        if (inserted != validRows) throw new IllegalStateException("날짜별 군 식단 검증/삽입 개수가 다릅니다.");
        return inserted;
    }

    private int validate(Path path) {
        if (path == null || !Files.isRegularFile(path)) throw new IllegalArgumentException("날짜별 군 식단 파일을 찾을 수 없습니다: " + path);
        int valid = 0;
        try (BufferedReader reader = reader(path)) {
            List<String> header = readCsvRow(reader);
            if (!HEADER.equals(header)) throw new IllegalArgumentException("날짜별 군 식단 필수 컬럼이 올바르지 않습니다: " + header);
            List<String> row;
            while ((row = readCsvRow(reader)) != null) if (parse(row) != null) valid++;
        } catch (IOException e) {
            throw new IllegalStateException("날짜별 군 식단 파일 검증 실패: " + path, e);
        }
        if (valid < 1000) throw new IllegalArgumentException("날짜별 군 식단 유효 행이 너무 적습니다: " + valid);
        return valid;
    }

    private BufferedReader reader(Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8), 64 * 1024);
    }

    private DailyRow parse(List<String> row) {
        if (row.size() != HEADER.size()) return null;
        try {
            double kcal = Double.parseDouble(row.get(5));
            int samples = Integer.parseInt(row.get(6));
            if (row.subList(0, 5).stream().anyMatch(String::isBlank) || kcal <= 0 || kcal > 2500 || samples <= 0) return null;
            return new DailyRow(row.get(0), LocalDate.parse(row.get(1)), row.get(2), row.get(3), row.get(4), kcal, samples);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<String> readCsvRow(Reader reader) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int value;
        boolean readAny = false;
        while ((value = reader.read()) != -1) {
            readAny = true;
            char ch = (char) value;
            if (quoted) {
                if (ch == '"') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') field.append('"');
                    else { quoted = false; if (next != -1) reader.reset(); }
                } else field.append(ch);
            } else if (ch == '"') quoted = true;
            else if (ch == ',') { fields.add(field.toString()); field.setLength(0); }
            else if (ch == '\n') { fields.add(field.toString()); return fields; }
            else if (ch != '\r') field.append(ch);
        }
        if (!readAny) return null;
        fields.add(field.toString());
        return fields;
    }

    private record DailyRow(String unitCode, LocalDate mealDate, String mealType, String searchName,
                            String canonicalName, double calorieKcal, int sampleCount) { }
}
