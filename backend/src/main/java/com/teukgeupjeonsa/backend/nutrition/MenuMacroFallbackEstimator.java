package com.teukgeupjeonsa.backend.nutrition;

import java.util.Optional;

/**
 * 식품 DB와 재료 추정에서 탄·단·지를 얻지 못한 군 급식 메뉴를 위한 최종 fallback.
 * 군 식단의 공식 칼로리는 변경하지 않고 메뉴명에서 판별한 음식군의 열량 비율만
 * 탄수화물·단백질·지방 g으로 환산한다.
 */
final class MenuMacroFallbackEstimator {

    private MenuMacroFallbackEstimator() {
    }

    static Optional<Estimate> estimate(String searchName, double officialCalorieKcal) {
        if (searchName == null || searchName.isBlank() || officialCalorieKcal <= 0) {
            return Optional.empty();
        }
        Profile profile = classify(searchName);
        return Optional.of(new Estimate(
                round1(officialCalorieKcal * profile.carbRatio / 4.0),
                round1(officialCalorieKcal * profile.proteinRatio / 4.0),
                round1(officialCalorieKcal * profile.fatRatio / 9.0),
                profile.label
        ));
    }

    private static Profile classify(String name) {
        if (containsAny(name, "전", "튀김", "완자", "까스", "가스")) {
            return new Profile("전·튀김류", 0.32, 0.28, 0.40);
        }
        if (containsAny(name, "국", "탕", "찌개", "짜글이")) {
            if (containsAny(name, "떡", "만두", "감자", "분모자")) {
                return new Profile("전분 포함 국·탕류", 0.48, 0.22, 0.30);
            }
            return new Profile("국·탕·찌개류", 0.32, 0.28, 0.40);
        }
        if (containsAny(name, "닭", "돈육", "돼지고기", "소고기", "쇠고기", "육")) {
            return new Profile("육류 요리", 0.18, 0.36, 0.46);
        }
        if (containsAny(name, "연어", "오징어", "해물", "새우", "생선", "젓")) {
            return new Profile("수산물 요리", 0.16, 0.40, 0.44);
        }
        if (containsAny(name, "우유", "요구르트", "요거트", "치즈")) {
            return new Profile("유제품·유제품 간식", 0.46, 0.22, 0.32);
        }
        if (containsAny(name, "떡", "초코", "가루", "과자", "빵", "케이크", "음료")) {
            return new Profile("간식·후식류", 0.70, 0.10, 0.20);
        }
        if (containsAny(name, "김치", "깍두기", "파김치", "겉절이")) {
            return new Profile("김치류", 0.62, 0.18, 0.20);
        }
        if (containsAny(name, "나물", "무침", "오이", "부추", "양배추", "버섯", "채소", "무")) {
            return new Profile("채소 반찬류", 0.55, 0.18, 0.27);
        }
        if (containsAny(name, "볶음", "조림", "찜", "구이", "스테이크")) {
            return new Profile("일반 조리 반찬", 0.30, 0.30, 0.40);
        }
        return new Profile("일반 혼합 음식", 0.45, 0.25, 0.30);
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    record Estimate(double carbohydrateG, double proteinG, double fatG, String category) {
    }

    private record Profile(String label, double carbRatio, double proteinRatio, double fatRatio) {
    }
}
