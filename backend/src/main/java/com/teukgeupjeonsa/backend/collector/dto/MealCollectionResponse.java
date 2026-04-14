package com.teukgeupjeonsa.backend.collector.dto;

import java.util.List;

public record MealCollectionResponse(
        boolean success,
        int totalFound,
        int matched,
        int downloaded,
        int failed,
        List<String> downloadedFiles,
        List<String> skippedTitles,
        List<String> failedTitles
) {
}
