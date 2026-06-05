package com.teukgeupjeonsa.backend.user;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 50)
    private String nickname;

    @Size(max = 500)
    private String profileImageUrl;

    @Positive
    private Double heightCm;

    @Positive
    private Double weightKg;

    @PastOrPresent
    private LocalDate enlistmentDate;
}
