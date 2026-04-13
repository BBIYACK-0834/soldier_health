package com.teukgeupjeonsa.backend.collector.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ParsedMealRow(
        String branch,
        String unitName,
        LocalDate mealDate,
        String breakfast,
        String lunch,
        String dinner,
        String rawRowHash
) {
}
