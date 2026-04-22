package com.teukgeupjeonsa.backend.unit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MealMenuCandidatesResponse {
    private String mealType;
    private LocalDate date;
    private int candidateCount;
    private List<String> menus;
}
