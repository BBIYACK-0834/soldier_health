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
    public static class MealNutritionItemResponse {
        private String menuName;
        private String normalizedName;
        private Boolean matched;
        private String matchedFoodName;
        private String displayCategory;
        private String matchType;
        private MatchConfidence confidence;
        private Double servingGram;
        private Integer calorieKcal;
        private Double carbohydrateG;
        private Double proteinG;
        private Double fatG;

        // 기존 프론트 호환 필드
        private Long foodId;
        private String foodName;
        private String mealType;
        private String category;
        private String servingUnit;
        private Integer calories;
        private Double carbG;
        private Double calorieSharePct;
        private Boolean addedByUser;
        private String matchStatus;
    }

    @Getter
    @Builder
    public static class MealNutritionResponse {
        private String mealType;
        private Integer officialCalorieKcal;
        private Integer estimatedCalorieKcal;
        private Integer matchedItemCount;
        private Integer totalItemCount;
        private Double matchedRatio;
        private List<MealNutritionItemResponse> items;

        // 기존 프론트 호환 필드
        private String mealLabel;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
    }

    @Getter
    @Builder
    public static class TodayMealNutritionResponse {
        private Integer totalCalories;
        private Integer totalOfficialCalories;
        private Integer totalEstimatedCalories;
        private Double totalProteinG;
        private Double totalCarbG;
        private Double totalFatG;
        private List<MealNutritionResponse> meals;
    }

    @Getter
    @Builder
    public static class FoodSearchResponse {
        private String query;
        private String normalizedQuery;
        private List<FoodSearchItemResponse> results;
    }

    @Getter
    @Builder
    public static class FoodSearchItemResponse {
        private Long foodMasterId;
        private String representativeName;
        private String displayCategory;
        private Double kcalPer100g;
        private Double carbohydratePer100g;
        private Double proteinPer100g;
        private Double fatPer100g;
        private Double defaultServingGram;
        private Double estimatedKcalForDefaultServing;
        private Long matchedAliasCount;
        private MatchConfidence confidence;
        private String qualityFlag;

        // 기존 프론트 호환 필드
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
        private java.util.Map<Long, Double> servingGramByFoodId;
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
