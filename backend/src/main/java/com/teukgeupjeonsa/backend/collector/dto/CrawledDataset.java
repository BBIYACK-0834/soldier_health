package com.teukgeupjeonsa.backend.collector.dto;

import lombok.Builder;

@Builder
public record CrawledDataset(
        String title,
        String sourceUrl,
        String provider,
        String description,
        String format
) {
}
