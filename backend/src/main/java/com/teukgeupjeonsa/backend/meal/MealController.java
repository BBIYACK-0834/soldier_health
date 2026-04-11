package com.teukgeupjeonsa.backend.meal;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @GetMapping("/today")
    public ApiResponse<MealDtos.MealDayResponse> getToday(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(mealService.getToday(user.getId()));
    }

    @GetMapping
    public ApiResponse<MealDtos.MealDayResponse> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(mealService.getByDate(user.getId(), date));
    }

    @GetMapping("/week")
    public ApiResponse<List<MealDtos.MealDayResponse>> getWeek(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ApiResponse.ok(mealService.getWeek(user.getId(), startDate));
    }
}
