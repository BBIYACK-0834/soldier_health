package com.teukgeupjeonsa.backend.unit;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MatchUnitByMealRequest {

    @NotNull(message = "날짜를 선택해주세요.")
    private LocalDate date;

    private String breakfast;
    private String lunch;
    private String dinner;
}
