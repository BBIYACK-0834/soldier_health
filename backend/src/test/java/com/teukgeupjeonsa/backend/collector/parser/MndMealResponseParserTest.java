package com.teukgeupjeonsa.backend.collector.parser;

import com.teukgeupjeonsa.backend.collector.util.MealMenuTextCleaner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MndMealResponseParserTest {

    private final MndMealResponseParser parser = new MndMealResponseParser(new MealMenuTextCleaner());

    @Test
    void duplicateBreakfastCaloriesAreCountedOnce() {
        List<MndMealResponseParser.ParsedMealRow> rows = parser.parseRows("SVC", response(
                row("20260611", "A", "954"),
                row("20260611", "A", "954")
        ));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).breakfastKcal()).isEqualTo(954);
    }

    @Test
    void differentBreakfastCaloriesUseMax() {
        List<MndMealResponseParser.ParsedMealRow> rows = parser.parseRows("SVC", response(
                row("20260611", "A", "900"),
                row("20260611", "A", "954")
        ));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).breakfastKcal()).isEqualTo(954);
    }

    @SafeVarargs
    private Map<String, Object> response(Map<String, Object>... rows) {
        return Map.of("SVC", Map.of("row", List.of(rows)));
    }

    private Map<String, Object> row(String date, String breakfast, String kcal) {
        return Map.of("MLSV_YMD", date, "BRKFST", breakfast, "brst_cal", kcal);
    }
}
