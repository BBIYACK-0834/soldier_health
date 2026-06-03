package com.teukgeupjeonsa.backend.user;

import lombok.Builder;
import lombok.Getter;

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
}
