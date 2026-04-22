package com.teukgeupjeonsa.backend.unit;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchedUnitResponse {
    private Long unitId;
    private String unitName;
    private String branchType;
    private String regionName;
    private double matchScore;
    private List<String> matchedMeals;
    private MealPreview mealPreview;
    private MealMatchDetail mealMatchDetail;

    @Getter
    @Builder
    public static class MealPreview {
        private String breakfast;
        private String lunch;
        private String dinner;
    }

    @Getter
    @Builder
    public static class MealMatchDetail {
        private double breakfastScore;
        private double lunchScore;
        private double dinnerScore;
    }
}
