package com.teukgeupjeonsa.backend.collector.controller;

import com.teukgeupjeonsa.backend.collector.dto.MealApiCollectResponse;
import com.teukgeupjeonsa.backend.collector.dto.MealCollectionSummary;
import com.teukgeupjeonsa.backend.collector.service.MealOpenApiCollectionService;
import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/collect/meals/openapi")
@RequiredArgsConstructor
public class MealOpenApiAdminController {

    private final MealOpenApiCollectionService mealOpenApiCollectionService;

    @PostMapping
    public ApiResponse<MealApiCollectResponse> collectAll() {
        MealCollectionSummary summary = mealOpenApiCollectionService.collectAllFromList();
        return MealApiCollectResponse.from(summary);
    }

    @PostMapping("/{unitName}")
    public ApiResponse<MealApiCollectResponse> collectByUnitName(@PathVariable String unitName) {
        MealCollectionSummary summary = mealOpenApiCollectionService.collectByUnitName(unitName);
        return MealApiCollectResponse.from(summary);
    }

    @PostMapping("/service/{serviceName}")
    public ApiResponse<MealApiCollectResponse> collectByServiceName(@PathVariable String serviceName) {
        MealCollectionSummary summary = mealOpenApiCollectionService.collectByServiceName(serviceName);
        return MealApiCollectResponse.from(summary);
    }
}
