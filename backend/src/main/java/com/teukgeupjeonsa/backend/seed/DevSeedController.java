package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.meal.service.MealImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/seed")
@RequiredArgsConstructor
public class DevSeedController {

    private final SeedService seedService;
    private final MealImportService mealImportService;

    @PostMapping("/sample-data")
    public ApiResponse<String> seedSampleData() {
        return ApiResponse.ok(seedService.seedSampleData());
    }

    @PostMapping("/sample-meals")
    public ApiResponse<String> seedSampleMeals() {
        return ApiResponse.ok(seedService.seedSampleMeals());
    }

    @PostMapping("/mnd-meals/import")
    public ApiResponse<MealImportService.ImportSummary> importMealsFromMndOpenApi() {
        return ApiResponse.ok(mealImportService.importAll());
    }
}
