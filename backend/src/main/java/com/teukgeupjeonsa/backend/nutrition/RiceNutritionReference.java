package com.teukgeupjeonsa.backend.nutrition;

import java.util.Set;

/**
 * 군 급식의 일반 밥은 원본 식단 칼로리를 확정값으로 사용하고,
 * 탄수화물·단백질·지방만 표준 백미밥 구성비로 추정한다.
 *
 * 기준(공식 칼로리 100 kcal당): 탄수화물 22.2g, 단백질 1.8g, 지방 0.2g.
 * 특정 foods 행의 품질이나 제공량에 영향을 받지 않는 추적 가능한 fallback이다.
 */
final class RiceNutritionReference {
    private static final Set<String> PLAIN_RICE_NAMES =
            Set.of("밥", "쌀밥", "백미밥", "잡곡밥", "흰밥", "현미밥", "멥쌀밥");
    private static final double CARBOHYDRATE_PER_100_KCAL = 22.2;
    private static final double PROTEIN_PER_100_KCAL = 1.8;
    private static final double FAT_PER_100_KCAL = 0.2;

    private RiceNutritionReference() {
    }

    static boolean supports(String searchName) {
        return searchName != null && PLAIN_RICE_NAMES.contains(searchName);
    }

    static Macros estimate(double officialCalorieKcal) {
        double scale = Math.max(0.0, officialCalorieKcal) / 100.0;
        return new Macros(
                round1(CARBOHYDRATE_PER_100_KCAL * scale),
                round1(PROTEIN_PER_100_KCAL * scale),
                round1(FAT_PER_100_KCAL * scale)
        );
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    record Macros(double carbohydrateG, double proteinG, double fatG) {
    }
}
