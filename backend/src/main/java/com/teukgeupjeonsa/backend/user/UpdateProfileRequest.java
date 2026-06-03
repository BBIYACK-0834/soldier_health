package com.teukgeupjeonsa.backend.user;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Positive
    private Double heightCm;

    @Positive
    private Double weightKg;
}
