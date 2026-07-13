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
    private String profileImageUrl;
    private Double heightCm;
    private Double weightKg;
    private Double targetWeight;
    private LocalDate birthDate;
    private String gender;
    private GoalType goalType;
    private WorkoutLevel workoutLevel;
    private Integer workoutDaysPerWeek;
    private Integer preferredWorkoutMinutes;
    private BranchType branchType;
    private String rank;
    private LocalDate dischargeDate;
    private LocalDate promotionDate;
    private LocalDate enlistmentDate;
    private LocalDate nextPromotionDate;
    private Long daysUntilDischarge;
    private Double serviceProgressPercent;
    private Long unitId;
    private String unitName;
    private UnitResponse unit;
}
