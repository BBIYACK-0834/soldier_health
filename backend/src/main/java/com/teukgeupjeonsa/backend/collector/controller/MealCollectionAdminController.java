package com.teukgeupjeonsa.backend.collector.controller;

import com.teukgeupjeonsa.backend.collector.dto.MealCollectionResponse;
import com.teukgeupjeonsa.backend.collector.service.MealCollectionService;
import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/collect/meals/csv")
@RequiredArgsConstructor
public class MealCollectionAdminController {

    private final MealCollectionService mealCollectionService;

    @PostMapping("/download")
    public ApiResponse<MealCollectionResponse> collectAndDownloadMealCsv() {
        return ApiResponse.ok(mealCollectionService.collectAndDownload());
    }
}
