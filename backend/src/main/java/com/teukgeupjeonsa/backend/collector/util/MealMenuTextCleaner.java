package com.teukgeupjeonsa.backend.collector.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MealMenuTextCleaner {

    private static final Pattern ALLERGY_CODE_PATTERN = Pattern.compile("\\((0?[1-9]|1[0-9]|2[0-9])\\)");

    /**
     * 국방부 급식 OpenAPI 메뉴명에 포함된 알레르기 번호만 제거한다.
     * 숫자 알레르기 코드가 아닌 괄호 정보는 화면 표시와 영양 매칭 힌트로 보존한다.
     */
    public String cleanMealText(String menuText) {
        if (menuText == null || menuText.isBlank()) {
            return menuText;
        }

        String cleaned = ALLERGY_CODE_PATTERN.matcher(menuText).replaceAll("");
        cleaned = cleaned.replaceAll("\\s*,\\s*", ", ");
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        cleaned = cleaned.replaceAll("^\\s*,\\s*", "");
        cleaned = cleaned.replaceAll("\\s*,\\s*$", "");
        cleaned = cleaned.trim();

        return cleaned.isBlank() ? null : cleaned;
    }
}
