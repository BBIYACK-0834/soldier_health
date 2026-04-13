package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.meal.PublicMealImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dev/seed")
@RequiredArgsConstructor
public class DevSeedController {

    private final SeedService seedService;
    private final PublicMealImportService publicMealImportService;

    @PostMapping("/sample-data")
    public ApiResponse<String> seedSampleData() {
        return ApiResponse.ok(seedService.seedSampleData());
    }

    @PostMapping("/sample-meals")
    public ApiResponse<String> seedSampleMeals() {
        return ApiResponse.ok(seedService.seedSampleMeals());
    }


    @PostMapping("/public-meals/all")
    public ApiResponse<String> importAllPublicMeals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(publicMealImportService.importFromPublicApi(startDate, endDate, null));
    }

    @PostMapping("/public-meals")
    public ApiResponse<String> importPublicMeals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String unitKeyword
    ) {
        return ApiResponse.ok(publicMealImportService.importFromPublicApi(startDate, endDate, unitKeyword));
    }
}
