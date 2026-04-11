package com.teukgeupjeonsa.backend.alarm;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class WorkoutAlarmController {

    private final WorkoutAlarmService workoutAlarmService;

    @GetMapping("/me")
    public ApiResponse<List<AlarmDtos.AlarmResponse>> getMyAlarms(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(workoutAlarmService.getMyAlarms(user.getId()));
    }

    @PostMapping
    public ApiResponse<AlarmDtos.AlarmResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AlarmDtos.AlarmSaveRequest request
    ) {
        return ApiResponse.ok(workoutAlarmService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlarmDtos.AlarmResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AlarmDtos.AlarmSaveRequest request
    ) {
        return ApiResponse.ok(workoutAlarmService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        workoutAlarmService.delete(user.getId(), id);
        return ApiResponse.ok("삭제 완료");
    }
}
