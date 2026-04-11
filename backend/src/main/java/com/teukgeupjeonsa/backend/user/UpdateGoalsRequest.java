package com.teukgeupjeonsa.backend.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGoalsRequest {
    @NotNull
    private GoalType goalType;

    @NotNull
    private WorkoutLevel workoutLevel;

    @NotNull
    private BranchType branchType;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer workoutDaysPerWeek;

    @NotNull
    @Min(10)
    @Max(180)
    private Integer preferredWorkoutMinutes;
}
