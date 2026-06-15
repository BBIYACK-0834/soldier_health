package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final FoodNameNormalizer normalizer;
    private final FoodMatchOverrideProvider overrideProvider;
    private final ServingEstimator servingEstimator;

    @Transactional(readOnly = true)
    public NutritionDtos.FoodSearchResponse search(String query) {
        String rawQuery = Optional.ofNullable(query).orElse("").trim();
        String normalizedQuery = normalizer.toSearchName(rawQuery);
        if (normalizedQuery.isBlank()) {
            return NutritionDtos.FoodSearchResponse.builder()
                    .query(rawQuery)
                    .normalizedQuery(normalizedQuery)
                    .results(List.of())
                    .build();
        }

        Map<Long, RankedFood> grouped = new LinkedHashMap<>();

        overrideProvider.findOverride(normalizedQuery)
                .ifPresent(override -> putBest(grouped, override.getFood(), "OVERRIDE_EXACT", MatchConfidence.HIGH, 1.0, override.getDefaultServingGram()));

        foodAliasRepository.findBySearchName(normalizedQuery).forEach(alias ->
                putBest(grouped, alias.getFood(), "ALIAS_EXACT", MatchConfidence.HIGH, 0.99, null));

        foodRepository.findFirstBySearchNameOrderBySourceCountDesc(normalizedQuery)
                .ifPresent(food -> putBest(grouped, food, "FOOD_EXACT", MatchConfidence.HIGH, 0.98, null));

        if (!isShortRiskyQuery(normalizedQuery)) {
            foodAliasRepository.searchContains(normalizedQuery, PageRequest.of(0, 80)).forEach(alias ->
                    putBest(grouped, alias.getFood(), "ALIAS_CONTAINS", MatchConfidence.MEDIUM, containsScore(normalizedQuery, alias.getSearchName()), null));
            foodRepository.searchContains(normalizedQuery, PageRequest.of(0, 80)).forEach(food ->
                    putBest(grouped, food, "FOOD_CONTAINS", MatchConfidence.MEDIUM, containsScore(normalizedQuery, food.getSearchName()), null));
        }

        List<NutritionDtos.FoodSearchItemResponse> results = grouped.values().stream()
                .filter(ranked -> isSafeForQuery(normalizedQuery, ranked.food()))
                .sorted(Comparator.comparingDouble(RankedFood::score).reversed()
                        .thenComparing(ranked -> Optional.ofNullable(ranked.food().getSourceCount()).orElse(0), Comparator.reverseOrder()))
                .limit(20)
                .map(ranked -> toItem(ranked, normalizedQuery))
                .toList();

        return NutritionDtos.FoodSearchResponse.builder()
                .query(rawQuery)
                .normalizedQuery(normalizedQuery)
                .results(results)
                .build();
    }

    private void putBest(Map<Long, RankedFood> grouped, Food food, String matchType, MatchConfidence confidence, double score, Double servingOverride) {
        if (food == null || food.getId() == null) {
            return;
        }
        RankedFood current = grouped.get(food.getId());
        RankedFood candidate = new RankedFood(food, matchType, confidence, score, servingOverride);
        if (current == null || candidate.score() > current.score()) {
            grouped.put(food.getId(), candidate);
        }
    }

    private NutritionDtos.FoodSearchItemResponse toItem(RankedFood ranked, String normalizedQuery) {
        Food food = ranked.food();
        double defaultServingGram = ranked.servingOverride() != null
                ? ranked.servingOverride()
                : servingEstimator.estimateGram(food.getName(), food.getCategory());
        Double estimatedKcal = multiply(food.getCalorie(), defaultServingGram / 100.0);
        long aliasCount = Math.max(Optional.ofNullable(food.getSourceCount()).orElse(0), foodAliasRepository.countByFood_Id(food.getId()));
        return NutritionDtos.FoodSearchItemResponse.builder()
                .foodMasterId(food.getId())
                .representativeName(food.getName())
                .displayCategory(food.getCategory())
                .kcalPer100g(food.getCalorie())
                .carbohydratePer100g(food.getCarbohydrate())
                .proteinPer100g(food.getProtein())
                .fatPer100g(food.getFat())
                .defaultServingGram(defaultServingGram)
                .estimatedKcalForDefaultServing(estimatedKcal)
                .matchedAliasCount(aliasCount)
                .confidence(ranked.confidence())
                .qualityFlag(food.getQualityFlag())
                .id(food.getId())
                .foodName(food.getName())
                .category(food.getCategory())
                .servingUnit(food.getServingUnit())
                .calories(estimatedKcal == null ? null : (int) Math.round(estimatedKcal))
                .proteinG(multiply(food.getProtein(), defaultServingGram / 100.0))
                .carbG(multiply(food.getCarbohydrate(), defaultServingGram / 100.0))
                .fatG(multiply(food.getFat(), defaultServingGram / 100.0))
                .matchedName(normalizedQuery)
                .build();
    }

    private boolean isSafeForQuery(String query, Food food) {
        if ("밥".equals(query) || "쌀밥".equals(query)) {
            String haystack = (safe(food.getName()) + safe(food.getSearchName()) + safe(food.getCategory())).replaceAll("\\s+", "");
            return (haystack.contains("밥") || haystack.contains("쌀") || haystack.contains("백미") || haystack.contains("현미") || haystack.contains("잡곡"))
                    && !containsAny(haystack, "아이스크림", "빙수", "음료", "주스", "디저트", "초코", "케이크");
        }
        return true;
    }

    private boolean isShortRiskyQuery(String query) {
        return query.length() <= 1 && !"밥".equals(query);
    }

    private double containsScore(String query, String target) {
        String value = safe(target).replaceAll("\\s+", "").toLowerCase();
        if (value.equals(query)) return 1.0;
        if (value.contains(query)) return query.length() / (double) Math.max(value.length(), 1);
        return 0.5;
    }

    private Double multiply(Double value, double scale) {
        return value == null ? null : Math.round(value * scale * 10.0) / 10.0;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RankedFood(Food food, String matchType, MatchConfidence confidence, double score, Double servingOverride) {
    }
}
