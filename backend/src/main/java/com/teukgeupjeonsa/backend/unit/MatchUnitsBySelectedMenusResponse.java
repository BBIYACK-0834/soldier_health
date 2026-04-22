package com.teukgeupjeonsa.backend.unit;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchUnitsBySelectedMenusResponse {
    private int candidateCount;
    private List<UnitMealMatchCard> units;

    @Getter
    @Builder
    public static class UnitMealMatchCard {
        private Long unitId;
        private String unitName;
        private String branchType;
        private String regionName;
        private int matchCount;
        private double matchScore;
        private String mealPreview;
    }
}
