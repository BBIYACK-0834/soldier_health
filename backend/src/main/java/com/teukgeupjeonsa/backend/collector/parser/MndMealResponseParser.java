package com.teukgeupjeonsa.backend.collector.parser;

import com.teukgeupjeonsa.backend.collector.dto.MndOpenApiDetailInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Component
public class MndMealResponseParser {

    private static final java.util.regex.Pattern KCAL_PATTERN = java.util.regex.Pattern.compile("([0-9]{2,5})\\s*(kcal|KCAL|㎉)?");

    private static final List<String> DATE_KEYS = List.of("MLSV_YMD", "DATE", "mealDate", "급식일자", "일자", "날짜");
    private static final List<String> BREAKFAST_KEYS = List.of("BRKFST", "조식", "breakfast", "조식메뉴");
    private static final List<String> LUNCH_KEYS = List.of("LUNCH", "중식", "lunch", "중식메뉴");
    private static final List<String> DINNER_KEYS = List.of("DINNER", "석식", "dinner", "석식메뉴");
    private static final List<String> BREAKFAST_KCAL_KEYS = List.of("BRKFST_KCAL", "BRKFST_CAL", "조식열량", "breakfastKcal");
    private static final List<String> LUNCH_KCAL_KEYS = List.of("LUNCH_KCAL", "LUNCH_CAL", "중식열량", "lunchKcal");
    private static final List<String> DINNER_KCAL_KEYS = List.of("DINNER_KCAL", "DINNER_CAL", "석식열량", "dinnerKcal");

    @SuppressWarnings("unchecked")
    public List<ParsedMealRow> parseRows(MndOpenApiDetailInfo detailInfo, Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return List.of();
        }

        Object serviceRoot = responseBody.get(detailInfo.serviceName());
        if (!(serviceRoot instanceof List<?> serviceRootList) || serviceRootList.size() < 2) {
            log.warn("서비스 루트 파싱 실패 serviceName={}", detailInfo.serviceName());
            return List.of();
        }

        Object rowContainer = serviceRootList.get(1);
        if (!(rowContainer instanceof Map<?, ?> rowMap)) {
            return List.of();
        }

        Object rows = rowMap.get("row");
        if (!(rows instanceof List<?> rowList)) {
            return List.of();
        }

        List<ParsedMealRow> result = new ArrayList<>();
        for (Object item : rowList) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            ParsedMealRow parsed = parseSingleRow((Map<String, Object>) map, detailInfo);
            if (parsed != null) {
                result.add(parsed);
            }
        }

        return result;
    }

    private ParsedMealRow parseSingleRow(Map<String, Object> row, MndOpenApiDetailInfo detailInfo) {
        LocalDate mealDate = parseDate(firstText(row, DATE_KEYS));
        if (mealDate == null) {
            log.warn("날짜 파싱 실패 row={}", row);
            return null;
        }

        String breakfastRaw = blankToNull(firstText(row, BREAKFAST_KEYS));
        String lunchRaw = blankToNull(firstText(row, LUNCH_KEYS));
        String dinnerRaw = blankToNull(firstText(row, DINNER_KEYS));

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

        return new ParsedMealRow(
                detailInfo.unitName(),
                detailInfo.serviceName(),
                mealDate,
                breakfastRaw,
                lunchRaw,
                dinnerRaw,
                breakfastKcal,
                lunchKcal,
                dinnerKcal
        );
    }

    private String firstText(Map<String, Object> row, List<String> aliases) {
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
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String compact = raw.replaceAll("[^0-9]", "");
        if (compact.matches("\\d{8}")) {
            try {
                return LocalDate.parse(compact, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        String normalized = raw.replace('.', '-').replace('/', '-');
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
        String number = raw.replaceAll("[^0-9-]", "");
        if (number.isBlank() || number.equals("-")) {
            return null;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private Integer parseKcalFromMealText(String mealText) {
        if (mealText == null || mealText.isBlank()) {
            return null;
        }

        java.util.regex.Matcher matcher = KCAL_PATTERN.matcher(mealText);
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

    public record ParsedMealRow(
            String unitName,
            String serviceName,
            LocalDate mealDate,
            String breakfastRaw,
            String lunchRaw,
            String dinnerRaw,
            Integer breakfastKcal,
            Integer lunchKcal,
            Integer dinnerKcal
    ) {
    }
}
