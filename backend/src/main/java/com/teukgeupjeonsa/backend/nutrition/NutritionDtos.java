package com.teukgeupjeonsa.backend.nutrition;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class NutritionDtos {

    @Getter
    @Builder
    public static class NutritionSummaryResponse {
        private int targetCalories;
        private double targetProteinG;
        private double targetCarbG;
        private double targetFatG;

        private int intakeCalories;
        private double intakeProteinG;
        private double intakeCarbG;
        private double intakeFatG;

        private int remainingCalories;
        private double remainingProteinG;
        private double remainingCarbG;
        private double remainingFatG;

        private double calorieProgressPct;
        private double proteinProgressPct;
        private double carbProgressPct;
        private double fatProgressPct;

        private double deficitProteinG;
        private double deficitCarbG;
        private double deficitFatG;

        private String note;
    }

    @Getter
    @Builder
    public static class FoodNutritionItemResponse {
        private Long id;
        private Long foodId;
        private String foodName;
        private String matchedFoodName;
        private String mealType;
        private String category;
        private String servingUnit;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
        private Double calorieSharePct;
        private Boolean addedByUser;
        private String matchStatus;
    }

    @Getter
    @Builder
    public static class MealNutritionDetailResponse {
        private String mealType;
        private String mealLabel;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
        private List<FoodNutritionItemResponse> items;
    }

    @Getter
    @Builder
    public static class TodayMealNutritionResponse {
        private Integer totalCalories;
        private Double totalProteinG;
        private Double totalCarbG;
        private Double totalFatG;
        private List<MealNutritionDetailResponse> meals;
    }

    @Getter
    @Builder
    public static class FoodSearchResponse {
        private Long id;
        private String foodName;
        private String category;
        private String servingUnit;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
        private String matchedName;
    }

    @Getter
    @Setter
    public static class AddMealFoodsRequest {
        private String mealType;
        private List<Long> foodIds;
    }

    @Getter
    @Builder
    public static class RecommendationResponse {
        private NutritionSummaryResponse summary;
        private List<String> ownedFoodSuggestions;
        private List<String> pxSuggestions;
        private String recommendationText;
    }

    @Getter
    @Setter
    public static class SaveOwnedFoodRequest {
        private String foodName;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
        private Integer quantity;
    }

    @Getter
    @Builder
    public static class OwnedFoodResponse {
        private Long id;
        private String foodName;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
        private Integer quantity;
    }
}
