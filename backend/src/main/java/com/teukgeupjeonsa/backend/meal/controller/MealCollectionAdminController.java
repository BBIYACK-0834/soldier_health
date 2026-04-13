package com.teukgeupjeonsa.backend.meal.controller;

import com.teukgeupjeonsa.backend.collector.dto.CollectionResult;
import com.teukgeupjeonsa.backend.collector.service.MealCollectionService;
import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/collect/meals")
@RequiredArgsConstructor
public class MealCollectionAdminController {

    private final MealCollectionService mealCollectionService;

    @PostMapping
    public ApiResponse<CollectionResult> collectMeals() {
        return ApiResponse.ok(mealCollectionService.collectAll());
    }
}
