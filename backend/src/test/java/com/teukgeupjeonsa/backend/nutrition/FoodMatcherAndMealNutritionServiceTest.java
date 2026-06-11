package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodMatcherAndMealNutritionServiceTest {

    private final FoodRepository foodRepository = mock(FoodRepository.class);
    private final FoodAliasRepository aliasRepository = mock(FoodAliasRepository.class);
    private final FoodNameNormalizer normalizer = new FoodNameNormalizer();
    private final FoodMatchOverrideProvider overrideProvider = mock(FoodMatchOverrideProvider.class);
    private final ServingDefaultRepository servingDefaultRepository = mock(ServingDefaultRepository.class);
    private final ServingEstimator servingEstimator = new ServingEstimator(servingDefaultRepository);
    private final FoodMatcher matcher = new FoodMatcher(foodRepository, aliasRepository, normalizer, overrideProvider, servingEstimator);

    @Test
    void riceAutoMatchIsRiceCategoryOnly() {
        Food rice = food(1L, "백미밥", "밥류", "백미밥", 150.0);
        Food dessert = food(2L, "밥아이스크림", "디저트류", "밥아이스크림", 250.0);
        when(overrideProvider.findOverride("밥")).thenReturn(Optional.empty());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc("밥")).thenReturn(Optional.of(alias(dessert, "밥")));
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("밥")).thenReturn(Optional.empty());
        when(foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(eq("밥"), eq("밥"), any(Pageable.class))).thenReturn(List.of(dessert, rice));

        FoodMatchResult result = matcher.match("밥");

        assertThat(result.isMatched()).isTrue();
        assertThat(result.getMatchedFoodName()).isEqualTo("백미밥");
        assertThat(result.getDisplayCategory()).isEqualTo("밥류");
    }

    @Test
    void mealNutritionKeepsOfficialAndEstimatedCaloriesSeparateAndLeavesNoMatchNull() {
        Food rice = food(1L, "백미밥", "밥류", "백미밥", 150.0);
        FoodMatchOverrideProvider provider = normalized -> {
            if ("밥".equals(normalized)) {
                return Optional.of(ManualFoodOverride.builder()
                        .rawMenuName("밥")
                        .normalizedMenuName("밥")
                        .food(rice)
                        .confidence(MatchConfidence.HIGH)
                        .defaultServingGram(200.0)
                        .build());
            }
            return Optional.empty();
        };
        FoodMatcher localMatcher = new FoodMatcher(foodRepository, aliasRepository, normalizer, provider, servingEstimator);
        MealNutritionService mealService = new MealNutritionService(normalizer, localMatcher, servingEstimator, new NutritionCalculator());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc(anyString())).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc(anyString())).thenReturn(Optional.empty());
        when(foodRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());

        NutritionDtos.MealNutritionResponse response = mealService.analyzeMeal("lunch", List.of("밥", "정체불명메뉴"), 900);

        assertThat(response.getOfficialCalorieKcal()).isEqualTo(900);
        assertThat(response.getEstimatedCalorieKcal()).isEqualTo(300);
        assertThat(response.getItems()).hasSize(2);
        NutritionDtos.MealNutritionItemResponse noMatch = response.getItems().get(1);
        assertThat(noMatch.getMatched()).isFalse();
        assertThat(noMatch.getCalorieKcal()).isNull();
        assertThat(noMatch.getCarbohydrateG()).isNull();
        assertThat(noMatch.getProteinG()).isNull();
        assertThat(noMatch.getFatG()).isNull();
    }

    private Food food(Long id, String name, String category, String searchName, Double kcal) {
        return Food.builder().id(id).name(name).category(category).searchName(searchName).servingUnit("100g")
                .calorie(kcal).carbohydrate(30.0).protein(3.0).fat(0.5).sourceCount(1).build();
    }

    private FoodAlias alias(Food food, String aliasName) {
        return FoodAlias.builder().food(food).aliasName(aliasName).originalName(aliasName).searchName(normalizer.toSearchName(aliasName)).category(food.getCategory()).build();
    }
}
