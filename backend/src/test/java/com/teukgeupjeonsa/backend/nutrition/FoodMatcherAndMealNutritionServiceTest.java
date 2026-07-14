package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.*;
import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionMatch;
import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionProvider;
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
    private final CompositeFoodEstimator compositeFoodEstimator = mock(CompositeFoodEstimator.class);
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
    void mealNutritionUsesOfficialCaloriesAsSingleMealCaloriesAndLeavesNoMatchNull() {
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
        MealNutritionService mealService = new MealNutritionService(normalizer, localMatcher, servingEstimator, new NutritionCalculator(), compositeFoodEstimator, new MealMenuItemParser());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc(anyString())).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc(anyString())).thenReturn(Optional.empty());
        when(foodRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());
        when(compositeFoodEstimator.estimate(anyString())).thenReturn(Optional.empty());

        NutritionDtos.MealNutritionResponse response = mealService.analyzeMeal("lunch", List.of("밥", "정체불명메뉴"), 900);

        assertThat(response.getOfficialCalorieKcal()).isEqualTo(900);
        assertThat(response.getEstimatedCalorieKcal()).isEqualTo(300);
        assertThat(response.getCalories()).isEqualTo(900);
        assertThat(response.getItems()).hasSize(2);
        NutritionDtos.MealNutritionItemResponse noMatch = response.getItems().get(1);
        assertThat(noMatch.getMatched()).isFalse();
        assertThat(noMatch.getCalorieKcal()).isNull();
        assertThat(noMatch.getCarbohydrateG()).isNull();
        assertThat(noMatch.getProteinG()).isNull();
        assertThat(noMatch.getFatG()).isNull();
        assertThat(response.getItems().get(0).getCalorieSharePct()).isEqualTo(33.3);
    }

    @Test
    void tokenFallbackMatchesCommonMilitaryMealNames() {
        Food kimchi = food(10L, "김치", "김치류", "김치", 35.0);
        when(overrideProvider.findOverride("배추김치")).thenReturn(Optional.empty());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc("배추김치")).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("배추김치")).thenReturn(Optional.empty());
        when(foodRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());
        when(aliasRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());
        when(foodRepository.searchContains(eq("김치"), any(Pageable.class))).thenReturn(List.of(kimchi));

        FoodMatchResult result = matcher.match("배추김치(09)");

        assertThat(result.isMatched()).isTrue();
        assertThat(result.getMatchedFoodName()).isEqualTo("김치");
        assertThat(result.getMatchType()).isEqualTo("TOKEN_CONTAINS");
    }

    @Test
    void compoundSashimiDoesNotMatchUnrelatedSnackAlias() {
        Food biscuit = food(11L, "비스킷", "간식", "비스킷", 145.0);
        when(overrideProvider.findOverride("파프리카오징어숙회")).thenReturn(Optional.empty());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc("파프리카오징어숙회")).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("파프리카오징어숙회")).thenReturn(Optional.empty());
        when(aliasRepository.searchContains(anyString(), any(Pageable.class)))
                .thenReturn(List.of(alias(biscuit, "파프리카 어니언 비스킷")));

        FoodMatchResult result = matcher.match("파프리카&오징어숙회");

        assertThat(result.isMatched()).isFalse();
    }

    @Test
    void reviewQualityFoodIsNotUsedForFuzzyMatch() {
        Food friedRice = food(12L, "볶음밥", "밥/분식", "볶음밥", 169.0);
        friedRice.setQualityFlag("review");
        when(overrideProvider.findOverride("깍두기반찬")).thenReturn(Optional.empty());
        when(aliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc("깍두기반찬")).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("깍두기반찬")).thenReturn(Optional.empty());
        when(foodRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of(friedRice));

        FoodMatchResult result = matcher.match("깍두기 반찬");

        assertThat(result.isMatched()).isFalse();
    }


    @Test
    void compositeEstimatorBuildsNutritionFromIngredientFoods() {
        Food pork = food(20L, "돼지고기", "육류", "돼지고기", 250.0);
        pork.setCarbohydrate(0.0);
        pork.setProtein(18.0);
        pork.setFat(19.0);
        Food kimchi = food(21L, "배추김치", "김치/장아찌류", "배추김치", 35.0);
        kimchi.setCarbohydrate(6.0);
        kimchi.setProtein(2.0);
        kimchi.setFat(0.5);

        when(servingDefaultRepository.findFirstByCategory(anyString())).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("돼지고기")).thenReturn(Optional.of(pork));
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("배추김치")).thenReturn(Optional.of(kimchi));
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("김치")).thenReturn(Optional.empty());
        when(aliasRepository.findBySearchName(anyString())).thenReturn(List.of());
        when(foodRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());
        when(aliasRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());

        CompositeFoodEstimator estimator = new CompositeFoodEstimator(foodRepository, aliasRepository, normalizer, servingDefaultRepository);
        Optional<CompositeFoodEstimateResult> result = estimator.estimate("돈육김치두루치기");

        assertThat(result).isPresent();
        assertThat(result.get().getConfidence()).isEqualTo(MatchConfidence.MEDIUM);
        assertThat(result.get().getMatchedDisplayName()).contains("돼지고기", "배추김치", "기반 추정");
        assertThat(result.get().getCalorieKcal()).isNotNull().isPositive();
        assertThat(result.get().getCarbohydrateG()).isNotNull();
        assertThat(result.get().getProteinG()).isNotNull().isPositive();
        assertThat(result.get().getFatG()).isNotNull().isPositive();
        assertThat(result.get().getIngredients()).hasSize(2);
    }

    @Test
    void compositeEstimatorPrefersRawPotatoOverSnackWithHigherSourceCount() {
        Food potatoChip = food(30L, "감자칩", "간식", "감자칩", 249.0);
        potatoChip.setSourceCount(34);
        potatoChip.setCarbohydrate(19.8);
        potatoChip.setProtein(1.8);
        potatoChip.setFat(11.1);
        Food boiledPotato = food(31L, "감자수미삶은것", "감자 및 전분류", "감자수미삶은것", 76.0);
        boiledPotato.setCarbohydrate(17.4);
        boiledPotato.setProtein(2.0);
        boiledPotato.setFat(0.0);

        when(servingDefaultRepository.findFirstByCategory(anyString())).thenReturn(Optional.empty());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("감자")).thenReturn(Optional.empty());
        when(foodRepository.findFirstByNameOrderBySourceCountDesc("감자")).thenReturn(Optional.empty());
        when(foodRepository.searchContains(eq("감자"), any(Pageable.class))).thenReturn(List.of(potatoChip, boiledPotato));
        when(aliasRepository.findBySearchName(anyString())).thenReturn(List.of());
        when(aliasRepository.searchContains(anyString(), any(Pageable.class))).thenReturn(List.of());

        CompositeFoodEstimator estimator = new CompositeFoodEstimator(foodRepository, aliasRepository, normalizer, servingDefaultRepository);
        CompositeFoodEstimateResult result = estimator.estimate("감자짜글이").orElseThrow();

        assertThat(result.getMatchedDisplayName()).contains("감자수미삶은것").doesNotContain("감자칩");
    }

    @Test
    void mealNutritionUsesCompositeEstimateAfterNoMatch() {
        FoodMatcher noMatchMatcher = mock(FoodMatcher.class);
        CompositeFoodEstimator estimator = mock(CompositeFoodEstimator.class);
        when(noMatchMatcher.match("주꾸미삼겹살볶음")).thenReturn(FoodMatchResult.noMatch("주꾸미삼겹살볶음", "주꾸미삼겹살볶음"));
        when(estimator.estimate("주꾸미삼겹살볶음")).thenReturn(Optional.of(CompositeFoodEstimateResult.builder()
                .originalMenuName("주꾸미삼겹살볶음")
                .normalizedMenuName("주꾸미삼겹살볶음")
                .cookingMethod("볶음")
                .servingGram(160.0)
                .calorieKcal(312)
                .carbohydrateG(8.2)
                .proteinG(24.1)
                .fatG(18.4)
                .confidence(MatchConfidence.MEDIUM)
                .matchedDisplayName("주꾸미 + 삼겹살 기반 추정")
                .ingredients(List.of())
                .build()));
        MealNutritionService mealService = new MealNutritionService(normalizer, noMatchMatcher, servingEstimator, new NutritionCalculator(), estimator, new MealMenuItemParser());

        NutritionDtos.MealNutritionResponse response = mealService.analyzeMeal("lunch", List.of("주꾸미삼겹살볶음"), null);

        NutritionDtos.MealNutritionItemResponse item = response.getItems().get(0);
        assertThat(item.getMatched()).isTrue();
        assertThat(item.getMatchType()).isEqualTo("COMPOSITE_ESTIMATE");
        assertThat(item.getMatchStatus()).isEqualTo("COMPOSITE_ESTIMATE");
        assertThat(item.getConfidence()).isEqualTo(MatchConfidence.MEDIUM);
        assertThat(item.getCalorieKcal()).isEqualTo(312);
        assertThat(item.getProteinG()).isEqualTo(24.1);
    }

    @Test
    void officialCaloriesAndEstimatedCaloriesAreSeparated() {
        MealNutritionService mealService = new MealNutritionService(normalizer, matcher, servingEstimator, new NutritionCalculator(), compositeFoodEstimator, new MealMenuItemParser());

        NutritionDtos.MealNutritionResponse response = mealService.buildResponse("breakfast", 954, List.of(
                NutritionDtos.MealNutritionItemResponse.builder()
                        .foodName("버거")
                        .matched(true)
                        .calorieKcal(500)
                        .calories(500)
                        .carbohydrateG(60.0)
                        .proteinG(20.0)
                        .fatG(18.0)
                        .build(),
                NutritionDtos.MealNutritionItemResponse.builder()
                        .foodName("감자튀김")
                        .matched(true)
                        .calorieKcal(620)
                        .calories(620)
                        .carbohydrateG(70.0)
                        .proteinG(8.0)
                        .fatG(30.0)
                        .build()
        ));

        assertThat(response.getOfficialCalorieKcal()).isEqualTo(954);
        assertThat(response.getEstimatedCalorieKcal()).isEqualTo(1120);
        assertThat(response.getCalories()).isEqualTo(954);
        assertThat(response.getItems()).extracting(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .containsExactly(426, 528);
    }

    @Test
    void officialCaloriesReconcileItemsAndMacrosForModerateUnderestimate() {
        MealNutritionService mealService = new MealNutritionService(normalizer, matcher, servingEstimator, new NutritionCalculator(), compositeFoodEstimator, new MealMenuItemParser());
        List<NutritionDtos.MealNutritionItemResponse> items = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            items.add(NutritionDtos.MealNutritionItemResponse.builder()
                    .foodName("메뉴" + i).matched(true).calorieKcal(i == 0 ? 297 : 100).calories(i == 0 ? 297 : 100)
                    .carbohydrateG(10.0).proteinG(5.0).fatG(2.0).addedByUser(false).build());
        }
        items.add(NutritionDtos.MealNutritionItemResponse.builder().foodName("미매칭").matched(false).addedByUser(false).build());

        NutritionDtos.MealNutritionResponse response = mealService.buildResponse("lunch", 1083, items);

        assertThat(response.getEstimatedCalorieKcal()).isEqualTo(797);
        assertThat(response.getItems().stream().map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum()).isEqualTo(1083);
        assertThat(response.getItems().stream().map(NutritionDtos.MealNutritionItemResponse::getCalorieSharePct)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum()).isBetween(99.9, 100.1);
        assertThat(response.getCarbG()).isGreaterThan(60.0);
    }

    @Test
    void consumptionMultiplierOnlyScalesProvidedMealAndNotUserAddedFood() {
        MealNutritionService mealService = new MealNutritionService(normalizer, matcher, servingEstimator, new NutritionCalculator(), compositeFoodEstimator, new MealMenuItemParser());
        NutritionDtos.MealNutritionResponse response = mealService.buildResponse("lunch", 800, List.of(
                NutritionDtos.MealNutritionItemResponse.builder().foodName("급식").matched(true)
                        .calorieKcal(800).calories(800).carbohydrateG(100.0).proteinG(30.0).fatG(20.0).addedByUser(false).build(),
                NutritionDtos.MealNutritionItemResponse.builder().foodName("추가 간식").matched(true)
                        .calorieKcal(200).calories(200).carbohydrateG(20.0).proteinG(10.0).fatG(8.0).addedByUser(true).build()
        ), 0.5);

        assertThat(response.getConsumedCalories()).isEqualTo(600);
        assertThat(response.getConsumedCarbG()).isEqualTo(70.0);
        assertThat(response.getConsumedProteinG()).isEqualTo(25.0);
        assertThat(response.getConsumedFatG()).isEqualTo(18.0);
    }

    @Test
    void calorieRoundingDifferenceIsDistributedAcrossItems() {
        MealNutritionService mealService = new MealNutritionService(normalizer, matcher, servingEstimator,
                new NutritionCalculator(), compositeFoodEstimator, new MealMenuItemParser());
        List<NutritionDtos.MealNutritionItemResponse> items = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> NutritionDtos.MealNutritionItemResponse.builder()
                        .foodName("메뉴" + index).matched(true).calorieKcal(1).calories(1)
                        .carbohydrateG(1.0).proteinG(1.0).fatG(1.0).addedByUser(false).build())
                .toList();

        NutritionDtos.MealNutritionResponse response = mealService.buildResponse("dinner", 6, items);

        assertThat(response.getItems()).extracting(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .containsExactly(1, 1, 2, 2);
        assertThat(response.getItems().stream().mapToInt(NutritionDtos.MealNutritionItemResponse::getCalorieKcal).sum())
                .isEqualTo(6);
    }

    @Test
    void unitMenuProfileTakesPriorityAndNormalizesContractLabels() {
        FoodMatcher noMatchMatcher = mock(FoodMatcher.class);
        when(noMatchMatcher.match(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return FoodMatchResult.noMatch(name, normalizer.normalize(name));
        });
        when(compositeFoodEstimator.estimate(anyString())).thenReturn(Optional.empty());
        MilitaryMenuNutritionProvider menuProvider = (serviceCode, mealDate, mealType, rawMenuName) ->
                "DS_TB_MNDT_DATEBYMLSVC_7296".equals(serviceCode)
                        && "깍두기".equals(normalizer.toSearchName(rawMenuName))
                        ? Optional.of(new MilitaryMenuNutritionMatch(
                                "깍두기", "김치류", 15.2, MatchConfidence.MEDIUM,
                                "UNIT_MENU_PROFILE", 112, "7296"))
                        : Optional.empty();
        MealNutritionService mealService = new MealNutritionService(
                normalizer, noMatchMatcher, servingEstimator, new NutritionCalculator(),
                compositeFoodEstimator, new MealMenuItemParser(), menuProvider);

        NutritionDtos.MealNutritionResponse response = mealService.analyzeMeal(
                "DS_TB_MNDT_DATEBYMLSVC_7296", "dinner", List.of("깍두기(수의계약)"), null);

        NutritionDtos.MealNutritionItemResponse item = response.getItems().get(0);
        assertThat(item.getMatched()).isTrue();
        assertThat(item.getMatchedFoodName()).isEqualTo("깍두기");
        assertThat(item.getMatchType()).isEqualTo("UNIT_MENU_PROFILE");
        assertThat(item.getCalorieKcal()).isEqualTo(15);
    }

    private Food food(Long id, String name, String category, String searchName, Double kcal) {
        return Food.builder().id(id).name(name).category(category).searchName(searchName).servingUnit("100g")
                .calorie(kcal).carbohydrate(30.0).protein(3.0).fat(0.5).sourceCount(1).build();
    }

    private FoodAlias alias(Food food, String aliasName) {
        return FoodAlias.builder().food(food).aliasName(aliasName).originalName(aliasName).searchName(normalizer.toSearchName(aliasName)).category(food.getCategory()).build();
    }
}
