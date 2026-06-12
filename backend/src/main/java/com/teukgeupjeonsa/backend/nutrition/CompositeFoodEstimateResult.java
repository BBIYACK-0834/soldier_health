package com.teukgeupjeonsa.backend.nutrition;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompositeFoodEstimateResult {
    private String originalMenuName;
    private String normalizedMenuName;
    private String cookingMethod;
    private double servingGram;
    private Integer calorieKcal;
    private Double carbohydrateG;
    private Double proteinG;
    private Double fatG;
    private MatchConfidence confidence;
    private String matchedDisplayName;
    private List<CompositeIngredientContribution> ingredients;
}
