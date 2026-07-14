package com.teukgeupjeonsa.backend.nutrition;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FoodNameNormalizer {

    private static final List<String> OPERATION_WORDS = List.of(
            "수의계약", "부대계약", "부대 계약", "완제품", "임가공", "간부용",
            "계약", "수의", "군납", "급식", "배식", "후식", "선택", "자율", "제공",
            "행사", "증정", "연간", "소외", "농심"
    );

    public String normalize(String menuName) {
        String normalized = Optional.ofNullable(menuName).orElse("")
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\(\\s*\\d{1,2}\\s*\\)", " ")
                .replaceAll("\\(\\s*완\\s*\\)", " ")
                .replaceAll("\\(\\s*\\d{1,2}\\s*[~～-]\\s*\\d{1,2}월\\s*\\)", " ")
                .replaceAll("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:ml|g|kg)\\b(?:\\s*/\\s*(?:팩|캔|병))?", " ")
                .replaceAll("\\d+(?:\\.\\d+)?\\s*㎖(?:\\s*/\\s*(?:팩|캔|병))?", " ")
                .replaceAll("[()\\[\\]{}]", " ")
                .replaceAll("(?<![0-9가-힣A-Za-z])\\d{1,2}(?![0-9가-힣A-Za-z])", " ")
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
        return normalize(value)
                .replace("쇠고기", "소고기")
                .replace("돈육", "돼지고기")
                .replace("계육", "닭고기")
                .replace("쭈꾸미", "주꾸미")
                .replace("생선묵", "어묵")
                .replace("만두국", "만둣국")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }
}
