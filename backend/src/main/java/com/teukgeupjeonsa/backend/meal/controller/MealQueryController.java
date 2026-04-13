package com.teukgeupjeonsa.backend.meal.controller;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.meal.entity.MilitaryMeal;
import com.teukgeupjeonsa.backend.meal.service.MealQueryService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealQueryController {

    private final MealQueryService mealQueryService;

    @GetMapping("/today")
    public ApiResponse<MealResponse> today(@RequestParam String unitName) {
        return ApiResponse.ok(toResponse(mealQueryService.getToday(unitName)));
    }

    @GetMapping
    public ApiResponse<MealResponse> byDate(@RequestParam String unitName,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(toResponse(mealQueryService.getByDate(unitName, date)));
    }

    @GetMapping("/units")
    public ApiResponse<List<String>> units() {
        return ApiResponse.ok(mealQueryService.getCollectedUnits());
    }

    private MealResponse toResponse(MilitaryMeal meal) {
        return MealResponse.builder()
                .sourceId(meal.getSource().getId())
                .unitName(meal.getUnitName())
                .branch(meal.getBranch())
                .mealDate(meal.getMealDate())
                .breakfast(meal.getBreakfast())
                .lunch(meal.getLunch())
                .dinner(meal.getDinner())
                .build();
    }

    @Value
    @Builder
    public static class MealResponse {
        Long sourceId;
        String unitName;
        String branch;
        LocalDate mealDate;
        String breakfast;
        String lunch;
        String dinner;
    }
}
