package com.teukgeupjeonsa.backend.user;

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
    private GoalType goalType;
    private WorkoutLevel workoutLevel;
    private Integer workoutDaysPerWeek;
    private Integer preferredWorkoutMinutes;
    private BranchType branchType;
    private LocalDate enlistmentDate;
    private LocalDate dischargeDate;
    private String rank;
    private LocalDate nextPromotionDate;
    private Long daysUntilDischarge;
    private Double serviceProgressPercent;
}
