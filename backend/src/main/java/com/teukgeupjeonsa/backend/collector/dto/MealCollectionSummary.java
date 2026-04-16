package com.teukgeupjeonsa.backend.collector.dto;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record MealCollectionSummary(
        boolean success,
        int totalFound,
        int detailParsed,
        int apiSucceeded,
        int apiFailed,
        int insertedRows,
        int updatedRows,
        List<String> skippedUnits,
        List<String> failedUnits
) {
    public static MealCollectionSummary empty() {
        return MealCollectionSummary.builder()
                .success(true)
                .totalFound(0)
                .detailParsed(0)
                .apiSucceeded(0)
                .apiFailed(0)
                .insertedRows(0)
                .updatedRows(0)
                .skippedUnits(new ArrayList<>())
                .failedUnits(new ArrayList<>())
                .build();
    }
}
