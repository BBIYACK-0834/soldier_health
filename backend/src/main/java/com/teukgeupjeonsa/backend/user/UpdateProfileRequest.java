package com.teukgeupjeonsa.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank
    private String nickname;

    @Positive
    private Double heightCm;

    @Positive
    private Double weightKg;
}
