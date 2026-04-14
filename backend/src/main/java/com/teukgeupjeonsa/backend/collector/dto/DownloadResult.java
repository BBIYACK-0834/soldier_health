package com.teukgeupjeonsa.backend.collector.dto;

public record DownloadResult(
        String title,
        String detailUrl,
        String csvDownloadUrl,
        String savedPath,
        boolean downloaded,
        boolean skipped,
        String reason
) {
}
