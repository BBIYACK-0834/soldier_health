package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final UnitMealMatchService unitMealMatchService;

    @GetMapping("/api/units")
    public ApiResponse<List<UnitResponse>> getUnits() {
        return ApiResponse.ok(unitService.getUnits(null));
    }

    @GetMapping("/api/units/search")
    public ApiResponse<List<UnitResponse>> searchUnits(@RequestParam String keyword) {
        return ApiResponse.ok(unitService.getUnits(keyword));
    }

    @PostMapping("/api/units/match-by-meal")
    public ApiResponse<List<MatchedUnitResponse>> matchByMeal(@Valid @RequestBody MatchUnitByMealRequest request) {
        return ApiResponse.ok(unitMealMatchService.matchByMeal(request));
    }

    @PostMapping("/api/users/me/unit")
    public ApiResponse<UnitResponse> setMyUnit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SetMyUnitRequest request
    ) {
        return ApiResponse.ok(unitService.setMyUnit(user.getId(), request.getUnitId()));
    }

    @GetMapping("/api/users/me/unit")
    public ApiResponse<UnitResponse> getMyUnit(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(unitService.getMyUnit(user.getId()));
    }
}
