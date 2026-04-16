package com.teukgeupjeonsa.backend.collector.dto;

import java.time.LocalDate;

public record MndOpenApiDetailInfo(
        String unitName,
        String serviceName,
        String openApiBaseUrl,
        String detailUrl,
        String provider,
        LocalDate updatedAt,
        String title
) {
}
