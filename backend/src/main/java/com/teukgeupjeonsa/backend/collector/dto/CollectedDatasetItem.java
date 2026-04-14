package com.teukgeupjeonsa.backend.collector.dto;

public record CollectedDatasetItem(
        String title,
        String detailUrl,
        String provider,
        String modifiedAt
) {
}
