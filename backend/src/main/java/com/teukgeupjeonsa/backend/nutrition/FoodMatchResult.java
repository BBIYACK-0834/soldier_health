package com.teukgeupjeonsa.backend.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.teukgeupjeonsa.backend.food.Food;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FoodMatchResult {
    private String originalMenuName;
    private String normalizedMenuName;
    private boolean matched;
    private Long matchedFoodId;
    private String matchedFoodName;
    private String matchType;
    private MatchConfidence confidence;
    private Double score;

    @JsonIgnore
    private Food matchedFood;

    public static FoodMatchResult noMatch(String originalMenuName, String normalizedMenuName) {
        return FoodMatchResult.builder()
                .originalMenuName(originalMenuName)
                .normalizedMenuName(normalizedMenuName)
                .matched(false)
                .matchType("NO_MATCH")
                .confidence(MatchConfidence.NONE)
                .score(0.0)
                .build();
    }
}
