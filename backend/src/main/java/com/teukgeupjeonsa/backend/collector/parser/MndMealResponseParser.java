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

    private static final Pattern KCAL_IN_TEXT_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:kcal|㎉)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\d{4}[-.]\\d{1,2}[-.]\\d{1,2}");

    // 💡 키값 보강 완료
    private static final List<String> DATE_KEYS = List.of("MLSV_YMD", "DATE", "mealDate", "급식일자", "일자", "날짜", "급식일", "dates");
    private static final List<String> BREAKFAST_KEYS = List.of("BRKFST", "조식", "breakfast", "조식메뉴", "brst");
    private static final List<String> LUNCH_KEYS = List.of("LUNCH", "중식", "lunch", "중식메뉴", "lunc");
    private static final List<String> DINNER_KEYS = List.of("DINNER", "석식", "dinner", "석식메뉴", "dinr");
    private static final List<String> UNIT_NAME_KEYS = List.of("UNIT_NM", "UNIT_NAME", "unitName", "부대명");
    private static final List<String> REGION_KEYS = List.of("AREA_NM", "AREA_NAME", "region", "지역");

    public List<ParsedMealRow> parseRows(String serviceName, Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return List.of();
        Object serviceRoot = responseBody.get(serviceName);
        if (serviceRoot == null) return List.of();

        List<Map<String, Object>> rowMaps = extractRowMaps(serviceRoot, serviceName);
        List<ParsedMealRow> result = new ArrayList<>();

        for (Map<String, Object> row : rowMaps) {
            ParsedMealRow parsed = parseSingleRow(row, serviceName);
            if (parsed != null) result.add(parsed);
        }
        log.info("식단 row 파싱 완료 service={}, count={}", serviceName, result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRowMaps(Object serviceRoot, String serviceName) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (serviceRoot instanceof List<?> serviceRootList) {
            for (Object item : serviceRootList) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object rows = itemMap.get("row");
                    if (rows instanceof List<?> rowList) {
                        for (Object row : rowList) if (row instanceof Map<?, ?> rowMap) result.add((Map<String, Object>) rowMap);
                    }
                }
            }
        }
        if (!result.isEmpty()) return result;

        if (serviceRoot instanceof Map<?, ?> rootMap) {
            Object rows = rootMap.get("row");
            if (rows instanceof List<?> rowList) {
                for (Object row : rowList) if (row instanceof Map<?, ?> rowMap) result.add((Map<String, Object>) rowMap);
            }
            if (!result.isEmpty()) return result;

            for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                    Object nestedRows = nestedMap.get("row");
                    if (nestedRows instanceof List<?> rowList) {
                        for (Object row : rowList) if (row instanceof Map<?, ?> rowMap) result.add((Map<String, Object>) rowMap);
                    }
                }
            }
        }
        return result;
    }

    private ParsedMealRow parseSingleRow(Map<String, Object> row, String serviceName) {
        String dateText = firstText(row, DATE_KEYS);
        if (dateText == null) return null;
        
        LocalDate mealDate = parseDate(dateText);
        if (mealDate == null) return null;

        // 1. 칼로리 추출을 위한 원본 텍스트
        String rawBreakfast = blankToNull(firstText(row, BREAKFAST_KEYS));
        String rawLunch = blankToNull(firstText(row, LUNCH_KEYS));
        String rawDinner = blankToNull(firstText(row, DINNER_KEYS));

        Integer breakfastKcal = parseKcalFromMealText(rawBreakfast);
        Integer lunchKcal = parseKcalFromMealText(rawLunch);
        Integer dinnerKcal = parseKcalFromMealText(rawDinner);
        Integer totalKcal = sum(breakfastKcal, lunchKcal, dinnerKcal);

        // 2. ✨ 화면에 나갈 텍스트는 클리닝 진행 ✨ ( RAW 대응 로직 삭제됨 )
        String cleanBreakfast = cleanMealText(rawBreakfast);
        String cleanLunch = cleanMealText(rawLunch);
        String cleanDinner = cleanMealText(rawDinner);

        return new ParsedMealRow(
                serviceName, mealDate, 
                cleanBreakfast, cleanLunch, cleanDinner, 
                breakfastKcal, lunchKcal, dinnerKcal, totalKcal,
                blankToNull(firstText(row, UNIT_NAME_KEYS)),
                blankToNull(firstText(row, REGION_KEYS))
        );
    }

    // 💡 먼지 털어내는 청소기 메서드 (용량, 수식, 괄호 제거)
    private String cleanMealText(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw;
        cleaned = cleaned.replaceAll("(?i)IF\\([^)]+\\)", ""); // 엑셀 수식
        cleaned = cleaned.replaceAll("\\d{4}[-.]\\d{2}[-.]\\d{2}\\([가-힣]\\)", ""); // 날짜
        cleaned = cleaned.replaceAll("(?i)\\d+(?:\\.\\d+)?\\s*(?:kcal|㎉|ml|g|kg|l)", ""); // 단위 및 칼로리
        cleaned = cleaned.replaceAll("\\([^)]*(계약|상표|공급|업체|개입)[^)]*\\)", ""); // 불필요 괄호
        cleaned = cleaned.replaceAll("\\b0\\b", ""); // 0 제거
        cleaned = cleaned.replaceAll("[,\\s]+", " ").trim(); // 띄어쓰기 정리
        return cleaned.isBlank() ? null : cleaned;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("\\([^)]*\\)", "").replaceAll("\\s+", "");
        String compact = cleaned.replaceAll("[^0-9]", "");
        try {
            if (compact.matches("\\d{8}")) return LocalDate.parse(compact, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(cleaned.replace('.', '-').replace('/', '-'), DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    private Integer parseKcalFromMealText(String mealText) {
        if (mealText == null) return null;
        Matcher matcher = KCAL_IN_TEXT_PATTERN.matcher(mealText);
        Integer max = null;
        while (matcher.find()) {
            try {
                int value = (int) Math.round(Double.parseDouble(matcher.group(1)));
                if (max == null || value > max) max = value;
            } catch (Exception ignored) {}
        }
        return max;
    }

    private Integer sum(Integer... values) {
        int sum = 0;
        for (Integer v : values) if (v != null) sum += v;
        return sum == 0 ? null : sum;
    }

    private String firstText(Map<String, Object> row, List<String> aliases) {
        for (String alias : aliases) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (normalize(e.getKey()).equals(normalize(alias))) {
                    Object value = e.getValue();
                    if (value != null && !value.toString().isBlank()) return value.toString().trim();
                }
            }
        }
        return null;
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }

    public record ParsedMealRow(
            String serviceName, LocalDate mealDate, String breakfastRaw,
            String lunchRaw, String dinnerRaw, Integer breakfastKcal,
            Integer lunchKcal, Integer dinnerKcal, Integer totalKcal,
            String unitName, String regionName
    ) {}
}