package com.teukgeupjeonsa.backend.nutrition;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FoodNameNormalizer {

    private static final List<String> OPERATION_WORDS = List.of(
            "부대계약", "부대 계약", "계약", "군납", "급식", "배식", "후식", "선택", "자율", "제공",
            "완제품", "행사", "증정", "연간"
    );

    public String normalize(String menuName) {
        String normalized = Optional.ofNullable(menuName).orElse("")
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\(\\s*\\d{1,2}\\s*\\)", " ")
                .replaceAll("[()\\[\\]{}]", " ")
                .replaceAll("(?<![가-힣A-Za-z])\\d{1,2}(?![가-힣A-Za-z])", " ")
                .replaceAll("[★*•·]", " ")
                .replace("&", "")
                .replace("+", "")
                .replace('/', ' ');

        for (String word : OPERATION_WORDS) {
            normalized = normalized.replace(word, " ");
        }

        return normalized
                .replaceAll("[^0-9A-Za-z가-힣\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String toSearchName(String value) {
        return normalize(value).replaceAll("\\s+", "").toLowerCase();
    }
}
