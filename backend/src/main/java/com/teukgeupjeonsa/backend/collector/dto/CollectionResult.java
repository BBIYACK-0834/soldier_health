package com.teukgeupjeonsa.backend.collector.dto;

import lombok.Builder;

@Builder
public record CollectionResult(
        int exploredCount,
        int filteredCount,
        int downloadedCount,
        int parsedCount,
        int savedCount,
        int duplicateCount,
        int failedCount
) {
}
