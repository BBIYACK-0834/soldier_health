package com.teukgeupjeonsa.backend.collector.download;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealDatasetDownloadService {

    private final MealCollectorProperties properties;

    public Path downloadCsv(String downloadUrl) {
        try {
            Files.createDirectories(Path.of(properties.getDownloadDirectory()));
            String token = HexFormat.of().formatHex(downloadUrl.getBytes()).substring(0, 20);
            Path target = Path.of(properties.getDownloadDirectory(), "meal_" + token + ".csv");
            if (Files.exists(target)) {
                Instant cutoff = Instant.now().minus(properties.getSkipRedownloadHours(), ChronoUnit.HOURS);
                if (Files.getLastModifiedTime(target).toInstant().isAfter(cutoff)) {
                    log.info("기존 다운로드 재사용 file={}, url={}", target, downloadUrl);
                    return target;
                }
            }
            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("CSV 다운로드 성공 file={}, url={}", target, downloadUrl);
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("CSV 다운로드 실패: " + downloadUrl, e);
        }
    }
}
