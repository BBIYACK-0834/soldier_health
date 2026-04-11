package com.teukgeupjeonsa.backend.user;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(userService.getMyProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok(userService.updateProfile(user.getId(), request));
    }

    @PutMapping("/goals")
    public ApiResponse<UserProfileResponse> updateGoals(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateGoalsRequest request
    ) {
        return ApiResponse.ok(userService.updateGoals(user.getId(), request));
    }

    @PutMapping("/workout-preferences")
    public ApiResponse<UserProfileResponse> updateWorkoutPreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateGoalsRequest request
    ) {
        return ApiResponse.ok(userService.updateGoals(user.getId(), request));
    }
}
