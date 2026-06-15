package com.teukgeupjeonsa.backend.nutrition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompositeIngredientContribution {
    private Long foodId;
    private String ingredientName;
    private String matchedFoodName;
    private double ratio;
    private double gram;
    private Double calorieKcal;
    private Double carbohydrateG;
    private Double proteinG;
    private Double fatG;
}
