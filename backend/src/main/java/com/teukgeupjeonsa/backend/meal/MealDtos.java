package com.teukgeupjeonsa.backend.meal;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class MealDtos {

    @Getter
    @Builder
    public static class MealDayResponse {
        private Long id;
        private LocalDate mealDate;
        private String unitName;
        private String sourceName;
        private String serviceCode;
        private String breakfastRaw;
        private String lunchRaw;
        private String dinnerRaw;
        private Integer breakfastKcal;
        private Integer lunchKcal;
        private Integer dinnerKcal;
        private Integer totalKcal;
    }
}
