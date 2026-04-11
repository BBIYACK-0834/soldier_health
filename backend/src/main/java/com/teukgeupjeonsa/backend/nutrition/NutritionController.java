package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    @GetMapping("/api/nutrition/today")
    public ApiResponse<NutritionDtos.NutritionSummaryResponse> getToday(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(nutritionService.getTodaySummary(user.getId()));
    }

    @GetMapping("/api/nutrition/recommendation/today")
    public ApiResponse<NutritionDtos.RecommendationResponse> getTodayRecommendation(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(nutritionService.getTodayRecommendation(user.getId()));
    }

    @GetMapping("/api/users/me/owned-foods")
    public ApiResponse<List<NutritionDtos.OwnedFoodResponse>> getOwnedFoods(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(nutritionService.getOwnedFoods(user.getId()));
    }

    @PostMapping("/api/users/me/owned-foods")
    public ApiResponse<NutritionDtos.OwnedFoodResponse> saveOwnedFood(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NutritionDtos.SaveOwnedFoodRequest request
    ) {
        return ApiResponse.ok(nutritionService.saveOwnedFood(user.getId(), request));
    }

    @PutMapping("/api/users/me/owned-foods/{id}")
    public ApiResponse<NutritionDtos.OwnedFoodResponse> updateOwnedFood(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody NutritionDtos.SaveOwnedFoodRequest request
    ) {
        return ApiResponse.ok(nutritionService.updateOwnedFood(user.getId(), id, request));
    }

    @DeleteMapping("/api/users/me/owned-foods/{id}")
    public ApiResponse<String> deleteOwnedFood(@AuthenticationPrincipal User user, @PathVariable Long id) {
        nutritionService.deleteOwnedFood(user.getId(), id);
        return ApiResponse.ok("삭제 완료");
    }
}
