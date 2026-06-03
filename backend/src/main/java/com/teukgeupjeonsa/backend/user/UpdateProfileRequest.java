package com.teukgeupjeonsa.backend.user;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {

    @Positive
    private Double heightCm;

    @Positive
    private Double weightKg;

    private String rank;

    private LocalDate dischargeDate;

    private LocalDate promotionDate;
}
