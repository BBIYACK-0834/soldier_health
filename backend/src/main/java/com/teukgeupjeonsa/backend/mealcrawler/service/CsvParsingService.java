package com.teukgeupjeonsa.backend.mealcrawler.service;

import com.teukgeupjeonsa.backend.mealcrawler.entity.MealMenu;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CsvParsingService {

    private static final List<String> DATE_KEYS = List.of("날짜", "일자", "급식일자");
    private static final List<String> BREAKFAST_KEYS = List.of("조식", "아침");
    private static final List<String> LUNCH_KEYS = List.of("중식", "점심");
    private static final List<String> DINNER_KEYS = List.of("석식", "저녁");
    private static final List<String> BREAKFAST_KCAL_KEYS = List.of("조식열량", "조식 열량", "아침열량");
    private static final List<String> LUNCH_KCAL_KEYS = List.of("중식열량", "중식 열량", "점심열량");
    private static final List<String> DINNER_KCAL_KEYS = List.of("석식열량", "석식 열량", "저녁열량");
    private static final List<String> TOTAL_KCAL_KEYS = List.of("총열량", "총 열량", "합계열량");
    private static final List<String> SOURCE_NAME_KEYS = List.of("부대명", "기관명", "소속", "출처명");

    public List<MealMenu> parse(String infId, String defaultSourceName, byte[] csvBytes) {
        List<Charset> candidates = List.of(StandardCharsets.UTF_8, Charset.forName("MS949"), Charset.forName("EUC-KR"));

        for (Charset charset : candidates) {
            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(csvBytes), charset);
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setTrim(true)
                         .build()
                         .parse(reader)) {
                List<MealMenu> menus = parseRecords(infId, defaultSourceName, parser);
                log.info("CSV 파싱 성공 infId={}, charset={}, rows={}", infId, charset, menus.size());
                return menus;
            } catch (Exception e) {
                log.warn("CSV 파싱 재시도 infId={}, charset={}, message={}", infId, charset, e.getMessage());
            }
        }

        throw new IllegalStateException("CSV 파싱 실패 infId=" + infId);
    }

    private List<MealMenu> parseRecords(String infId, String defaultSourceName, CSVParser parser) {
        Map<String, Integer> headerMap = parser.getHeaderMap();

        String dateHeader = findHeader(headerMap, DATE_KEYS)
                .orElseThrow(() -> new IllegalArgumentException("날짜 컬럼을 찾지 못했습니다."));
        String breakfastHeader = findHeader(headerMap, BREAKFAST_KEYS).orElse(null);
        String lunchHeader = findHeader(headerMap, LUNCH_KEYS).orElse(null);
        String dinnerHeader = findHeader(headerMap, DINNER_KEYS).orElse(null);
        String breakfastKcalHeader = findHeader(headerMap, BREAKFAST_KCAL_KEYS).orElse(null);
        String lunchKcalHeader = findHeader(headerMap, LUNCH_KCAL_KEYS).orElse(null);
        String dinnerKcalHeader = findHeader(headerMap, DINNER_KCAL_KEYS).orElse(null);
        String totalKcalHeader = findHeader(headerMap, TOTAL_KCAL_KEYS).orElse(null);
        String sourceNameHeader = findHeader(headerMap, SOURCE_NAME_KEYS).orElse(null);

        List<MealMenu> rows = new ArrayList<>();
        for (CSVRecord record : parser.getRecords()) {
            LocalDate mealDate = parseDate(record.get(dateHeader));
            if (mealDate == null) {
                continue;
            }

            String sourceName = getValue(record, sourceNameHeader);
            if (sourceName == null || sourceName.isBlank()) {
                sourceName = defaultSourceName;
            }
            if (sourceName == null || sourceName.isBlank()) {
                sourceName = "국방부 식단";
            }

            rows.add(MealMenu.builder()
                    .sourceName(sourceName)
                    .infId(infId)
                    .mealDate(mealDate)
                    .breakfast(getValue(record, breakfastHeader))
                    .lunch(getValue(record, lunchHeader))
                    .dinner(getValue(record, dinnerHeader))
                    .breakfastKcal(parseInteger(getValue(record, breakfastKcalHeader)))
                    .lunchKcal(parseInteger(getValue(record, lunchKcalHeader)))
                    .dinnerKcal(parseInteger(getValue(record, dinnerKcalHeader)))
                    .totalKcal(parseInteger(getValue(record, totalKcalHeader)))
                    .build());
        }

        return rows;
    }

    private Optional<String> findHeader(Map<String, Integer> headerMap, List<String> aliases) {
        return headerMap.keySet().stream()
                .filter(Objects::nonNull)
                .filter(header -> aliases.stream().anyMatch(alias -> normalize(header).contains(normalize(alias))))
                .findFirst();
    }

    private String normalize(String raw) {
        return raw.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    private String getValue(CSVRecord record, String header) {
        if (header == null || !record.isMapped(header)) {
            return null;
        }
        String value = record.get(header);
        if (value == null) {
            return null;
        }
        value = value.replace("\r", "").trim();
        return value.isBlank() ? null : value;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9]", "");
        try {
            if (digits.matches("\\d{8}")) {
                return LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
            }
            String dashed = raw.replaceAll("\\.", "-").replaceAll("/", "-").replaceAll("\\s", "");
            if (dashed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dashed);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
