package com.teukgeupjeonsa.backend.collector.parser;

import com.teukgeupjeonsa.backend.collector.util.MealMenuTextCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Component
public class MndMealResponseParser {

    private final MealMenuTextCleaner mealMenuTextCleaner;

    public MndMealResponseParser(MealMenuTextCleaner mealMenuTextCleaner) {
        this.mealMenuTextCleaner = mealMenuTextCleaner;
    }

    private static final List<String> DATE_KEYS = List.of("MLSV_YMD", "DATE", "mealDate", "급식일자", "일자", "날짜", "급식일", "dates");
    private static final List<String> BREAKFAST_KEYS = List.of("BRKFST", "조식", "breakfast", "조식메뉴", "brst");
    private static final List<String> LUNCH_KEYS = List.of("LUNCH", "중식", "lunch", "중식메뉴", "lunc");
    private static final List<String> DINNER_KEYS = List.of("DINNER", "석식", "dinner", "석식메뉴", "dinr");
    private static final List<String> UNIT_NAME_KEYS = List.of("UNIT_NM", "UNIT_NAME", "unitName", "부대명");
    private static final List<String> REGION_KEYS = List.of("AREA_NM", "AREA_NAME", "region", "지역");
    private static final List<String> DAILY_TOTAL_CALORIE_KEYS = List.of(
            "sum_cal", "total_cal", "tot_cal", "total_kcal", "day_cal", "daily_cal",
            "일일총칼로리", "총칼로리", "합계칼로리");

    public List<ParsedMealRow> parseRows(String serviceName, Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return List.of();

        Object serviceRoot = responseBody.get(serviceName);
        if (serviceRoot == null) return List.of();

        List<Map<String, Object>> rowMaps = extractRowMaps(serviceRoot, serviceName);
        
        // 💡 핵심: 같은 날짜(LocalDate)의 식단과 칼로리를 하나로 묶기 위한 Map
        Map<LocalDate, CombinedMealData> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rowMaps) {
            String dateText = firstText(row, DATE_KEYS);
            if (dateText == null) continue;

            LocalDate mealDate = parseDate(dateText);
            if (mealDate == null) continue;

            // 각 행에서 메뉴와 칼로리 추출
            String brst = blankToNull(firstText(row, BREAKFAST_KEYS));
            String lunc = blankToNull(firstText(row, LUNCH_KEYS));
            String dinr = blankToNull(firstText(row, DINNER_KEYS));

            Double brstCal = parseCalValue(firstText(row, List.of("brst_cal", "BREAKFAST_CAL")));
            Double luncCal = parseCalValue(firstText(row, List.of("lunc_cal", "LUNCH_CAL")));
            Double dinrCal = parseCalValue(firstText(row, List.of("dinr_cal", "DINNER_CAL")));
            Double dailyTotalCal = parseCalValue(firstText(row, DAILY_TOTAL_CALORIE_KEYS));

            String unitName = blankToNull(firstText(row, UNIT_NAME_KEYS));
            String region = blankToNull(firstText(row, REGION_KEYS));

            // 같은 날짜 가 있으면 가져오고, 없으면 새로 생성
            CombinedMealData combinedData = groupedMap.computeIfAbsent(mealDate, d -> new CombinedMealData(unitName, region));
            
            // 데이터 누적 (글자는 청소해서 합치고, 칼로리는 더하기)
            combinedData.addBreakfast(mealMenuTextCleaner.cleanMealText(brst), brstCal);
            combinedData.addLunch(mealMenuTextCleaner.cleanMealText(lunc), luncCal);
            combinedData.addDinner(mealMenuTextCleaner.cleanMealText(dinr), dinrCal);
            combinedData.setDailyTotalCalorie(dailyTotalCal);
        }

        // 💡 묶은 데이터를 최종 Entity 변환용 DTO 리스트로 변환
        List<ParsedMealRow> result = new ArrayList<>();
        for (Map.Entry<LocalDate, CombinedMealData> entry : groupedMap.entrySet()) {
            LocalDate date = entry.getKey();
            CombinedMealData cd = entry.getValue();

            Integer bKcal = cd.getBreakfastKcal();
            Integer lKcal = cd.getLunchKcal();
            Integer dKcal = cd.getDinnerKcal();
            Integer tKcal = cd.getDailyTotalKcal();
            if (tKcal == null) {
                tKcal = sum(bKcal, lKcal, dKcal);
            }

            result.add(new ParsedMealRow(
                    serviceName, date,
                    cd.getBreakfastStr(), cd.getLunchStr(), cd.getDinnerStr(),
                    bKcal, lKcal, dKcal, tKcal,
                    cd.unitName, cd.regionName
            ));
        }

        log.info("🎯 식단 일자별 병합 완료 service={}, 총 {}일치 식단 정제됨", serviceName, result.size());
        return result;
    }

    // 💡 날짜별 메뉴 합산 및 칼로리 누적을 위한 내부 헬퍼 클래스
    private static class CombinedMealData {
        List<String> breakfastList = new ArrayList<>();
        List<String> lunchList = new ArrayList<>();
        List<String> dinnerList = new ArrayList<>();
        Map<String, Integer> breakfastCalories = new LinkedHashMap<>();
        Map<String, Integer> lunchCalories = new LinkedHashMap<>();
        Map<String, Integer> dinnerCalories = new LinkedHashMap<>();
        Integer dailyTotalKcal;
        String unitName;
        String regionName;

        CombinedMealData(String unitName, String regionName) {
            this.unitName = unitName;
            this.regionName = regionName;
        }

        void addBreakfast(String menu, Double cal) {
            addMenuWithCalorie(breakfastList, breakfastCalories, menu, cal);
        }
        void addLunch(String menu, Double cal) {
            addMenuWithCalorie(lunchList, lunchCalories, menu, cal);
        }
        void addDinner(String menu, Double cal) {
            addMenuWithCalorie(dinnerList, dinnerCalories, menu, cal);
        }

        void setDailyTotalCalorie(Double value) {
            if (value == null || value <= 0) return;
            int rounded = (int) Math.round(value);
            if (dailyTotalKcal == null || rounded > dailyTotalKcal) dailyTotalKcal = rounded;
        }

        String getBreakfastStr() { return breakfastList.isEmpty() ? null : String.join(", ", breakfastList); }
        String getLunchStr() { return lunchList.isEmpty() ? null : String.join(", ", lunchList); }
        String getDinnerStr() { return dinnerList.isEmpty() ? null : String.join(", ", dinnerList); }

        Integer getBreakfastKcal() { return sumOrNull(breakfastCalories); }
        Integer getLunchKcal() { return sumOrNull(lunchCalories); }
        Integer getDinnerKcal() { return sumOrNull(dinnerCalories); }
        Integer getDailyTotalKcal() { return dailyTotalKcal; }

        private void addMenuWithCalorie(List<String> menus, Map<String, Integer> calories,
                                        String menu, Double calorie) {
            if (menu == null || menu.isBlank()) return;
            if (!menus.contains(menu)) menus.add(menu);
            if (calorie != null && calorie > 0) {
                calories.putIfAbsent(menu, (int) Math.round(calorie));
            }
        }

        private Integer sumOrNull(Map<String, Integer> calories) {
            if (calories.isEmpty()) return null;
            return calories.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    // 💡 텍스트에서 순수 숫자 칼로리만 뽑아내는 메서드
    private Double parseCalValue(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            String cleaned = text.replaceAll("[^0-9.]", "");
            return cleaned.isBlank() ? null : Double.parseDouble(cleaned);
        } catch (Exception e) { return null; }
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

    private Integer sum(Integer... values) {
        int sum = 0; boolean hasValue = false;
        for (Integer v : values) { if (v != null) { sum += v; hasValue = true; } }
        return hasValue ? sum : null;
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