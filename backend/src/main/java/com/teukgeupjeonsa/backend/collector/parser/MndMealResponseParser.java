package com.teukgeupjeonsa.backend.collector.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MndMealResponseParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private static final Pattern KCAL_IN_TEXT_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:kcal|㎉)", Pattern.CASE_INSENSITIVE);

    private static final List<String> DATE_KEYS =
            List.of("MLSV_YMD", "DATE", "mealDate", "급식일자", "일자", "날짜", "급식일", "DATES", "dates");

    private static final List<String> BREAKFAST_KEYS =
            List.of("BRKFST", "조식", "breakfast", "조식메뉴", "조식내용", "BRST", "brst");

    private static final List<String> LUNCH_KEYS =
            List.of("LUNCH", "중식", "lunch", "중식메뉴", "중식내용", "LUNC", "lunc");

    private static final List<String> DINNER_KEYS =
            List.of("DINNER", "석식", "dinner", "석식메뉴", "석식내용", "DINR", "dinr");

    private static final List<String> BREAKFAST_KCAL_KEYS =
            List.of("BRKFST_KCAL", "BRKFST_CAL", "조식열량", "breakfastKcal", "조식칼로리", "BRST_CAL", "brst_cal");

    private static final List<String> LUNCH_KCAL_KEYS =
            List.of("LUNCH_KCAL", "LUNCH_CAL", "중식열량", "lunchKcal", "중식칼로리", "LUNC_CAL", "lunc_cal");

    private static final List<String> DINNER_KCAL_KEYS =
            List.of("DINNER_KCAL", "DINNER_CAL", "석식열량", "dinnerKcal", "석식칼로리", "DINR_CAL", "dinr_cal");

    private static final List<String> TOTAL_KCAL_KEYS =
            List.of("TOTAL_KCAL", "TOT_CAL", "총열량", "totalKcal", "열량합계", "총칼로리", "SUM_CAL", "sum_cal");

    private static final List<String> UNIT_NAME_KEYS =
            List.of("UNIT_NM", "UNIT_NAME", "unitName", "부대명", "부대", "군부대명");

    private static final List<String> REGION_KEYS =
            List.of("AREA_NM", "AREA_NAME", "region", "지역", "소재지");

    public List<ParsedMealRow> parseRows(String serviceName, Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return List.of();
        }

        Object serviceRoot = responseBody.get(serviceName);
        if (serviceRoot == null) {
            log.warn("서비스 키 없음 serviceName={}, responseKeys={}", serviceName, responseBody.keySet());
            return List.of();
        }

        List<Map<String, Object>> rowMaps = extractRowMaps(serviceRoot, serviceName);
        if (rowMaps.isEmpty()) {
            log.warn("row 추출 실패 serviceName={}, serviceRootType={}",
                    serviceName, serviceRoot.getClass().getName());
            return List.of();
        }

        List<ParsedMealRow> result = new ArrayList<>();
        for (Map<String, Object> rowMap : rowMaps) {
            ParsedMealRow parsed = parseSingleRow(rowMap, serviceName);
            if (parsed != null) {
                result.add(parsed);
            }
        }

        log.info("식단 row 파싱 완료 serviceName={}, parsedCount={}", serviceName, result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRowMaps(Object serviceRoot, String serviceName) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (serviceRoot instanceof List<?> serviceRootList) {
            for (Object item : serviceRootList) {
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }

                Map<String, Object> itemMap = (Map<String, Object>) itemMapRaw;
                Object rows = itemMap.get("row");

                if (rows instanceof List<?> rowList) {
                    for (Object rowItem : rowList) {
                        if (rowItem instanceof Map<?, ?> rowMapRaw) {
                            result.add((Map<String, Object>) rowMapRaw);
                        }
                    }
                }
            }

            if (!result.isEmpty()) {
                return result;
            }
        }

        if (serviceRoot instanceof Map<?, ?> rootMapRaw) {
            Map<String, Object> rootMap = (Map<String, Object>) rootMapRaw;
            Object rows = rootMap.get("row");

            if (rows instanceof List<?> rowList) {
                for (Object rowItem : rowList) {
                    if (rowItem instanceof Map<?, ?> rowMapRaw) {
                        result.add((Map<String, Object>) rowMapRaw);
                    }
                }
            }

            if (!result.isEmpty()) {
                return result;
            }

            for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
                Object nestedValue = entry.getValue();

                if (!(nestedValue instanceof Map<?, ?> nestedMapRaw)) {
                    continue;
                }

                Map<String, Object> nestedMap = (Map<String, Object>) nestedMapRaw;
                Object nestedRows = nestedMap.get("row");

                if (nestedRows instanceof List<?> nestedRowList) {
                    for (Object rowItem : nestedRowList) {
                        if (rowItem instanceof Map<?, ?> rowMapRaw) {
                            result.add((Map<String, Object>) rowMapRaw);
                        }
                    }
                }
            }

            if (!result.isEmpty()) {
                return result;
            }
        }

        log.warn("서비스 루트 파싱 실패 serviceName={}, serviceRoot={}", serviceName, serviceRoot);
        return result;
    }

    private ParsedMealRow parseSingleRow(Map<String, Object> row, String serviceName) {
        if (isCompletelyEmptyRow(row)) {
            return null;
        }

        String dateText = firstText(row, DATE_KEYS);
        String breakfastRaw = blankToNull(firstText(row, BREAKFAST_KEYS));
        String lunchRaw = blankToNull(firstText(row, LUNCH_KEYS));
        String dinnerRaw = blankToNull(firstText(row, DINNER_KEYS));

        if ((dateText == null || dateText.isBlank())
                && breakfastRaw == null
                && lunchRaw == null
                && dinnerRaw == null) {
            return null;
        }

        LocalDate mealDate = parseDate(dateText);
        if (mealDate == null) {
            log.warn("날짜 파싱 실패 serviceName={}, row={}", serviceName, row);
            return null;
        }

        Integer breakfastKcal = parseKcal(firstText(row, BREAKFAST_KCAL_KEYS));
        if (breakfastKcal == null) {
            breakfastKcal = parseKcalFromMealText(breakfastRaw);
        }

        Integer lunchKcal = parseKcal(firstText(row, LUNCH_KCAL_KEYS));
        if (lunchKcal == null) {
            lunchKcal = parseKcalFromMealText(lunchRaw);
        }

        Integer dinnerKcal = parseKcal(firstText(row, DINNER_KCAL_KEYS));
        if (dinnerKcal == null) {
            dinnerKcal = parseKcalFromMealText(dinnerRaw);
        }

        Integer totalKcal = parseKcal(firstText(row, TOTAL_KCAL_KEYS));
        String unitName = blankToNull(firstText(row, UNIT_NAME_KEYS));
        String regionName = blankToNull(firstText(row, REGION_KEYS));

        return new ParsedMealRow(
                serviceName,
                mealDate,
                breakfastRaw,
                lunchRaw,
                dinnerRaw,
                breakfastKcal,
                lunchKcal,
                dinnerKcal,
                totalKcal,
                unitName,
                regionName
        );
    }

    private boolean isCompletelyEmptyRow(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }

        for (Object value : row.values()) {
            if (value != null && !String.valueOf(value).trim().isBlank()) {
                return false;
            }
        }

        return true;
    }

    private String firstText(Map<String, Object> row, List<String> aliases) {
        if (row == null || aliases == null) {
            return null;
        }

        for (String alias : aliases) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (normalize(entry.getKey()).equals(normalize(alias))) {
                    Object value = entry.getValue();
                    if (value != null) {
                        String text = String.valueOf(value).trim();
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        }

        return null;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }

        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣]", "");
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String cleaned = raw.trim()
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\s+", "")
                .trim();

        String compact = cleaned.replaceAll("[^0-9]", "");
        if (compact.matches("\\d{8}")) {
            try {
                return LocalDate.parse(compact, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        String normalized = cleaned.replace('.', '-')
                .replace('/', '-');

        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private Integer parseKcal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.replace(',', '.');
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);

        if (!matcher.find()) {
            return null;
        }

        try {
            double value = Double.parseDouble(matcher.group());
            if (value < 0) {
                return null;
            }

            return (int) Math.round(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseKcalFromMealText(String mealText) {
        if (mealText == null || mealText.isBlank()) {
            return null;
        }

        Matcher matcher = KCAL_IN_TEXT_PATTERN.matcher(mealText);
        Integer maxValue = null;

        while (matcher.find()) {
            Integer kcal = parseKcal(matcher.group(1));
            if (kcal == null) {
                continue;
            }

            if (maxValue == null || kcal > maxValue) {
                maxValue = kcal;
            }
        }

        return maxValue;
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    public record ParsedMealRow(
            String serviceName,
            LocalDate mealDate,
            String breakfastRaw,
            String lunchRaw,
            String dinnerRaw,
            Integer breakfastKcal,
            Integer lunchKcal,
            Integer dinnerKcal,
            Integer totalKcal,
            String unitName,
            String regionName
    ) {
    }
}