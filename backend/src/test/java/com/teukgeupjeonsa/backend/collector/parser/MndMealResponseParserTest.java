package com.teukgeupjeonsa.backend.collector.parser;

import com.teukgeupjeonsa.backend.collector.util.MealMenuTextCleaner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MndMealResponseParserTest {

    private static final String SERVICE = "DS_TB_MNDT_DATEBYMLSVC_7296";
    private final MndMealResponseParser parser = new MndMealResponseParser(new MealMenuTextCleaner());

    @Test
    void sumsEachMenuOfficialCaloriesInsteadOfTakingTheLargestValue() {
        MndMealResponseParser.ParsedMealRow parsed = parser.parseRows(SERVICE, response(
                row("밥", "360kcal", "감자국", "74.85kcal", "김치", "14kcal", null),
                row("사골떡만둣국(01)", "507.13kcal", "불고기", "200.48kcal", "우유", "135kcal", null)
        )).get(0);

        assertThat(parsed.breakfastKcal()).isEqualTo(867);
        assertThat(parsed.lunchKcal()).isEqualTo(275);
        assertThat(parsed.dinnerKcal()).isEqualTo(149);
        assertThat(parsed.totalKcal()).isEqualTo(1291);
    }

    @Test
    void doesNotCountTheSameMenuRowTwice() {
        MndMealResponseParser.ParsedMealRow parsed = parser.parseRows(SERVICE, response(
                row("밥", "360kcal", null, null, null, null, null),
                row("밥", "360kcal", null, null, null, null, null)
        )).get(0);

        assertThat(parsed.breakfastKcal()).isEqualTo(360);
    }

    @Test
    void keepsTheRepeatedDailyTotalAsOneDateValue() {
        MndMealResponseParser.ParsedMealRow parsed = parser.parseRows(SERVICE, response(
                row("밥", "360kcal", "감자국", "74.85kcal", "김치", "14kcal", "3724.26kcal"),
                row("사골떡만둣국", "507.13kcal", "불고기", "200.48kcal", "우유", "135kcal", "3724.26kcal")
        )).get(0);

        assertThat(parsed.totalKcal()).isEqualTo(3724);
    }

    @SafeVarargs
    private Map<String, Object> response(Map<String, Object>... rows) {
        return Map.of(SERVICE, Map.of("row", List.of(rows)));
    }

    private Map<String, Object> row(String breakfast, String breakfastKcal,
                                    String lunch, String lunchKcal,
                                    String dinner, String dinnerKcal,
                                    String dailyTotalKcal) {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("dates", "2026-07-14(화)");
        if (breakfast != null) row.put("brst", breakfast);
        if (breakfastKcal != null) row.put("brst_cal", breakfastKcal);
        if (lunch != null) row.put("lunc", lunch);
        if (lunchKcal != null) row.put("lunc_cal", lunchKcal);
        if (dinner != null) row.put("dinr", dinner);
        if (dinnerKcal != null) row.put("dinr_cal", dinnerKcal);
        if (dailyTotalKcal != null) row.put("sum_cal", dailyTotalKcal);
        return row;
    }
}
