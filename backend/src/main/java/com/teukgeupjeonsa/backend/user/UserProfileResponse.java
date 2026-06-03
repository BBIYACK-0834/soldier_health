package com.teukgeupjeonsa.backend.user;

import com.teukgeupjeonsa.backend.unit.UnitResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private Double heightCm;
    private Double weightKg;
    private GoalType goalType;
    private WorkoutLevel workoutLevel;
    private Integer workoutDaysPerWeek;
    private Integer preferredWorkoutMinutes;
    private BranchType branchType;
    private String rank;
    private LocalDate dischargeDate;
    private LocalDate promotionDate;
    private Long unitId;
    private String unitName;
    private UnitResponse unit;
}
