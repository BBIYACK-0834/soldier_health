package com.teukgeupjeonsa.backend.workout;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutRecommendationService workoutRecommendationService;

    @GetMapping("/recommendation/today")
    public ApiResponse<WorkoutDtos.WorkoutRecommendationResponse> getToday(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(workoutRecommendationService.getTodayRecommendation(user.getId()));
    }

    @GetMapping("/plan")
    public ApiResponse<WorkoutDtos.WorkoutRecommendationResponse> getPlan(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(workoutRecommendationService.getTodayRecommendation(user.getId()));
    }
}
