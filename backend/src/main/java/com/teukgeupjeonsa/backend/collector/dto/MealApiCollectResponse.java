package com.teukgeupjeonsa.backend.collector.dto;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;

public record MealApiCollectResponse(
        boolean success,
        int totalFound,
        int detailParsed,
        int apiSucceeded,
        int apiFailed,
        int insertedRows,
        int updatedRows,
        int skippedUnits,
        int failedUnits
) {
    public static ApiResponse<MealApiCollectResponse> from(MealCollectionSummary summary) {
        MealApiCollectResponse response = new MealApiCollectResponse(
                summary.success(),
                summary.totalFound(),
                summary.detailParsed(),
                summary.apiSucceeded(),
                summary.apiFailed(),
                summary.insertedRows(),
                summary.updatedRows(),
                summary.skippedUnits().size(),
                summary.failedUnits().size()
        );
        return ApiResponse.ok(response);
    }
}
