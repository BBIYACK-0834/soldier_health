package com.teukgeupjeonsa.backend.collector.dto;

import java.time.LocalDate;

public record MndOpenApiListItem(
        String title,
        String detailUrl,
        String provider,
        LocalDate updatedAt,
        String unitNameCandidate
) {
}
