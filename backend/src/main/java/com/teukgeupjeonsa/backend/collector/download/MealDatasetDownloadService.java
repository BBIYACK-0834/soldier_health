package com.teukgeupjeonsa.backend.collector.download;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.dto.CollectedDatasetItem;
import com.teukgeupjeonsa.backend.collector.dto.DownloadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealDatasetDownloadService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MealCollectorBot/1.0)";

    private final MealCollectorProperties properties;

    public Optional<String> findCsvDownloadUrl(String detailUrl) {
        try {
            log.info("상세 페이지 접근 시작 url={}", detailUrl);
            Document document = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout(properties.getTimeoutMillis())
                    .get();

            Elements links = document.select("a[href], button[data-url], button[data-href]");
            for (Element element : links) {
                String href = element.attr("abs:href");
                if (href == null || href.isBlank()) {
                    href = element.attr("data-url");
                }
                if (href == null || href.isBlank()) {
                    href = element.attr("data-href");
                }
                if (href == null || href.isBlank()) {
                    href = toAbsoluteUrl(detailUrl, element.attr("href"));
                }
                if (href == null || href.isBlank()) {
                    continue;
                }

                String text = (element.text() + " " + element.attr("title") + " " + href).toLowerCase(Locale.ROOT);
                if (isCsvLink(href, text)) {
                    String absolute = toAbsoluteUrl(detailUrl, href);
                    log.info("CSV 링크 발견 detailUrl={}, csvUrl={}", detailUrl, absolute);
                    return Optional.ofNullable(absolute);
                }
            }

            log.info("CSV 링크 미발견 detailUrl={}", detailUrl);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상세 페이지 접근 실패 detailUrl={}", detailUrl, e);
            return Optional.empty();
        }
    }

    public DownloadResult downloadCsv(CollectedDatasetItem item) {
        Optional<String> csvUrlOptional = findCsvDownloadUrl(item.detailUrl());
        if (csvUrlOptional.isEmpty()) {
            return new DownloadResult(item.title(), item.detailUrl(), null, null, false, true, "csv-link-not-found");
        }

        String csvUrl = csvUrlOptional.get();
        Path directory = properties.ensureDownloadDirectory();

        try {
            String sanitizedTitle = sanitizeFilename(item.title());
            if (!sanitizedTitle.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                sanitizedTitle += ".csv";
            }
            Path filePath = directory.resolve(sanitizedTitle);
            filePath = avoidCollision(filePath);

            if (shouldSkipRecentFile(filePath)) {
                log.info("최근 다운로드 파일이 있어 skip title={}, path={}", item.title(), filePath);
                return new DownloadResult(item.title(), item.detailUrl(), csvUrl, filePath.toString(), false, true, "recent-file-exists");
            }

            try (InputStream inputStream = new URL(csvUrl).openStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("CSV 다운로드 성공 title={}, path={}, provider={}, modifiedAt={}",
                    item.title(), filePath, item.provider(), item.modifiedAt());

            return new DownloadResult(item.title(), item.detailUrl(), csvUrl, filePath.toString(), true, false, "downloaded");
        } catch (Exception e) {
            log.warn("CSV 다운로드 실패 title={}, csvUrl={}", item.title(), csvUrl, e);
            return new DownloadResult(item.title(), item.detailUrl(), csvUrl, null, false, false, "download-failed");
        }
    }

    private boolean shouldSkipRecentFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return false;
            }
            Instant cutoff = Instant.now().minus(properties.getSkipRedownloadHours(), ChronoUnit.HOURS);
            return Files.getLastModifiedTime(path).toInstant().isAfter(cutoff);
        } catch (Exception e) {
            return false;
        }
    }

    private Path avoidCollision(Path original) {
        if (!Files.exists(original)) {
            return original;
        }

        if (properties.getSkipRedownloadHours() > 0 && shouldSkipRecentFile(original)) {
            return original;
        }

        String fileName = original.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int sequence = 2;
        Path parent = original.getParent();
        while (true) {
            Path candidate = parent.resolve(baseName + "_" + sequence + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
            sequence++;
        }
    }

    private boolean isCsvLink(String href, String text) {
        String lowerHref = href.toLowerCase(Locale.ROOT);
        return lowerHref.endsWith(".csv")
                || lowerHref.contains("format=csv")
                || lowerHref.contains("type=csv")
                || text.contains(" csv")
                || text.contains("csv ")
                || text.contains("csv다운로드")
                || (text.contains("다운로드") && text.contains("csv"));
    }

    private String sanitizeFilename(String original) {
        String sanitized = original.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.isBlank()) {
            return "meal_dataset";
        }
        return sanitized;
    }

    private String toAbsoluteUrl(String baseUrl, String maybeRelative) {
        if (maybeRelative == null || maybeRelative.isBlank()) {
            return maybeRelative;
        }
        if (maybeRelative.startsWith("http://") || maybeRelative.startsWith("https://")) {
            return maybeRelative;
        }
        if (maybeRelative.startsWith("//")) {
            return "https:" + maybeRelative;
        }
        try {
            return URI.create(baseUrl).resolve(maybeRelative).toString();
        } catch (Exception e) {
            if (maybeRelative.startsWith("/")) {
                return properties.getBaseDomain() + maybeRelative;
            }
            return properties.getBaseDomain() + "/" + maybeRelative;
        }
    }
}
