package com.teukgeupjeonsa.backend.auth;

import com.teukgeupjeonsa.backend.user.BranchType;
import com.teukgeupjeonsa.backend.user.GoalType;
import com.teukgeupjeonsa.backend.user.WorkoutLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String nickname;

    @NotNull
    private GoalType goalType;

    @NotNull
    private WorkoutLevel workoutLevel;

    @NotNull
    private BranchType branchType;
}
