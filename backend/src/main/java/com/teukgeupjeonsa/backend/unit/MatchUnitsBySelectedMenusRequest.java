package com.teukgeupjeonsa.backend.unit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MatchUnitsBySelectedMenusRequest {

    @NotNull(message = "날짜를 선택해주세요.")
    private LocalDate date;

    @NotNull(message = "mealType을 선택해주세요.")
    private String mealType;

    @NotEmpty(message = "메뉴를 하나 이상 선택해주세요.")
    private List<String> selectedMenus;
}
