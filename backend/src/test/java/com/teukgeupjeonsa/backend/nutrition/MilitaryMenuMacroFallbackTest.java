package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionMatch;
import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MilitaryMenuMacroFallbackTest {

    private static final List<MenuCase> TODAY_MENUS = List.of(
            new MenuCase("사골떡만둣국", 507.13),
            new MenuCase("감자짜글이", 163.52),
            new MenuCase("새우살감자국", 74.85),
            new MenuCase("돈육굴소스양배추볶음", 250.60),
            new MenuCase("쇠고기버섯볶음", 200.48),
            new MenuCase("분모자마라찜닭", 309.66),
            new MenuCase("오징어젓무침", 34.62),
            new MenuCase("훈제연어스테이크크리미양파드레싱", 215.0),
            new MenuCase("해물완자전", 190.0),
            new MenuCase("배추김치", 14.0),
            new MenuCase("오이부추무침", 13.27),
            new MenuCase("들깨무나물", 36.33),
            new MenuCase("우유에타먹는가루", 353.0),
            new MenuCase("초코찰떡", 120.0),
            new MenuCase("깍두기", 16.0),
            new MenuCase("백색우유", 135.0),
            new MenuCase("파김치", 10.8)
    );

    @Test
    void everyProvidedTodayMenuGetsNonZeroEnergyConsistentMacros() {
        for (MenuCase menu : TODAY_MENUS) {
            MenuMacroFallbackEstimator.Estimate estimate =
                    MenuMacroFallbackEstimator.estimate(menu.name(), menu.kcal()).orElseThrow();

            assertThat(estimate.carbohydrateG()).as(menu.name()).isGreaterThanOrEqualTo(0.0);
            assertThat(estimate.proteinG()).as(menu.name()).isGreaterThan(0.0);
            assertThat(estimate.fatG()).as(menu.name()).isGreaterThan(0.0);
            double macroCalories = estimate.carbohydrateG() * 4
                    + estimate.proteinG() * 4 + estimate.fatG() * 9;
            assertThat(macroCalories).as(menu.name()).isCloseTo(menu.kcal(),
                    org.assertj.core.data.Offset.offset(1.0));
        }
    }

    @Test
    void mealServiceUsesCategoryFallbackWithoutReplacingOfficialCalories() {
        FoodMatcher matcher = mock(FoodMatcher.class);
        CompositeFoodEstimator composite = mock(CompositeFoodEstimator.class);
        ServingEstimator serving = mock(ServingEstimator.class);
        FoodNameNormalizer normalizer = new FoodNameNormalizer();
        when(matcher.match(anyString())).thenAnswer(invocation ->
                FoodMatchResult.noMatch(invocation.getArgument(0),
                        normalizer.normalize(invocation.getArgument(0))));
        when(composite.estimate(anyString())).thenReturn(Optional.empty());

        MilitaryMenuNutritionProvider provider = (serviceCode, mealDate, mealType, rawMenuName) ->
                Optional.of(new MilitaryMenuNutritionMatch(
                        rawMenuName, "군 급식 날짜별 관측", 507.13,
                        MatchConfidence.HIGH, "DAILY_UNIT_MENU", 1, "7296"));

        MealNutritionService service = new MealNutritionService(
                normalizer, matcher, serving, new NutritionCalculator(),
                composite, new MealMenuItemParser(), provider);

        NutritionDtos.MealNutritionItemResponse item = service.analyzeMeal(
                "DS_TB_MNDT_DATEBYMLSVC_7296", null, "breakfast",
                List.of("사골떡만둣국"), 507).getItems().get(0);

        assertThat(item.getCalorieKcal()).isEqualTo(507);
        assertThat(item.getCalorieSource()).isEqualTo("DAILY_UNIT_MENU");
        assertThat(item.getMacroSource()).isEqualTo("OFFICIAL_CALORIE_CATEGORY_ESTIMATE");
        assertThat(item.getCarbohydrateG()).isPositive();
        assertThat(item.getProteinG()).isPositive();
        assertThat(item.getFatG()).isPositive();
    }

    private record MenuCase(String name, double kcal) {
    }
}
