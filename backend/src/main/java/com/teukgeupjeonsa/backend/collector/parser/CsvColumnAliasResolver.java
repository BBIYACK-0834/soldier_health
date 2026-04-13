package com.teukgeupjeonsa.backend.collector.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CsvColumnAliasResolver {

    private static final Map<String, List<String>> CANDIDATES = Map.of(
            "unit", List.of("부대", "부대명", "unit", "unit_name", "기관명"),
            "date", List.of("일자", "날짜", "기준일", "meal_date", "급식일자", "date"),
            "breakfast", List.of("조식", "아침", "breakfast", "조식메뉴"),
            "lunch", List.of("중식", "점심", "lunch", "중식메뉴"),
            "dinner", List.of("석식", "저녁", "dinner", "석식메뉴")
    );

    public String resolve(Map<String, Integer> headerMap, String logicalName) {
        List<String> aliases = CANDIDATES.getOrDefault(logicalName, List.of());
        for (String key : headerMap.keySet()) {
            String normalized = normalize(key);
            if (aliases.stream().map(this::normalize).anyMatch(normalized::contains)) {
                return key;
            }
        }
        return null;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
    }
}
