package com.teukgeupjeonsa.backend.mealcrawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MndDataDownloadService {

    private static final String BASE = "https://opendata.mnd.go.kr";
    private static final Pattern CSV_PATH_PATTERN = Pattern.compile(
            "([\"'])(/[^\"']*(?:csv|download)[^\"']*)\\1", Pattern.CASE_INSENSITIVE);

    private final HttpRetryClient httpRetryClient;

    public Optional<byte[]> downloadCsv(String infId) {
        String sheetUrl = BASE + "/openinf/sheetview2.jsp?infId=" + encode(infId);
        log.info("[3/4] sheetview 페이지 요청: {}", sheetUrl);

        String html;
        try {
            html = httpRetryClient.getText(sheetUrl, Map.of("Referer", "https://www.data.go.kr"), 3).body();
        } catch (Exception e) {
            log.warn("sheetview 페이지 요청 실패 infId={}", infId, e);
            return Optional.empty();
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.addAll(extractCsvCandidatesFromSheet(sheetUrl, html, infId));
        candidates.addAll(defaultCsvCandidates(infId));

        for (String candidate : candidates) {
            try {
                log.info("CSV 후보 요청 infId={}, url={}", infId, candidate);
                var response = httpRetryClient.getBytes(candidate, Map.of(
                        "Referer", sheetUrl,
                        "Accept", "text/csv,application/octet-stream;q=0.9,*/*;q=0.8"
                ), 3);

                if (looksLikeCsv(response.headers().firstValue("Content-Type").orElse(""), response.body())) {
                    log.info("CSV 다운로드 성공 infId={}, bytes={}, url={}", infId, response.body().length, candidate);
                    return Optional.of(response.body());
                }
            } catch (Exception e) {
                log.debug("CSV 후보 실패 infId={}, url={}", infId, candidate, e);
            }
        }

        log.warn("CSV 다운로드 실패 infId={} (모든 후보 URL 실패)", infId);
        return Optional.empty();
    }

    private Set<String> extractCsvCandidatesFromSheet(String sheetUrl, String html, String infId) {
        Set<String> urls = new LinkedHashSet<>();
        Document document = Jsoup.parse(html, sheetUrl);

        for (Element e : document.select("a[href], button[onclick], script")) {
            urls.addAll(extractUrlLikeStrings(e.outerHtml()));
        }
        urls.addAll(extractUrlLikeStrings(html));

        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : urls) {
            String absolute = toAbsoluteUrl(BASE, raw);
            if (absolute == null || absolute.isBlank()) {
                continue;
            }
            if (!absolute.contains("infId=") && absolute.matches(".*(csv|download).*")) {
                absolute += (absolute.contains("?") ? "&" : "?") + "infId=" + encode(infId);
            }
            normalized.add(absolute);
        }
        return normalized;
    }

    private Set<String> extractUrlLikeStrings(String htmlChunk) {
        Set<String> results = new LinkedHashSet<>();
        Matcher pathMatcher = CSV_PATH_PATTERN.matcher(htmlChunk);
        while (pathMatcher.find()) {
            results.add(pathMatcher.group(2));
        }

        Pattern absPattern = Pattern.compile("https://opendata\\.mnd\\.go\\.kr[^\"'\\s<>]*(?:csv|download)[^\"'\\s<>]*",
                Pattern.CASE_INSENSITIVE);
        Matcher absMatcher = absPattern.matcher(htmlChunk);
        while (absMatcher.find()) {
            results.add(absMatcher.group());
        }
        return results;
    }

    private List<String> defaultCsvCandidates(String infId) {
        String encoded = encode(infId);
        return List.of(
                BASE + "/openinf/sheetview2.jsp?infId=" + encoded + "&download=csv",
                BASE + "/openinf/sheetview2.jsp?infId=" + encoded + "&fileType=csv",
                BASE + "/openinf/openapi/DownloadCsv.do?infId=" + encoded,
                BASE + "/openinf/openapi/OpenDataCsvDownload.do?infId=" + encoded,
                BASE + "/openinf/openapi/DownloadData.do?infId=" + encoded + "&fileType=csv",
                BASE + "/openinf/sheetview/downloadCsv.do?infId=" + encoded,
                BASE + "/openinf/sheetview/download.do?infId=" + encoded + "&fileType=csv"
        );
    }

    private boolean looksLikeCsv(String contentType, byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        String lowerCt = contentType.toLowerCase(Locale.ROOT);
        if (lowerCt.contains("csv") || lowerCt.contains("excel") || lowerCt.contains("octet-stream")) {
            return true;
        }

        String preview = new String(body, 0, Math.min(body.length, 400), StandardCharsets.UTF_8);
        return preview.contains(",") && preview.contains("\n");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String toAbsoluteUrl(String baseUrl, String maybeRelative) {
        if (maybeRelative == null || maybeRelative.isBlank()) {
            return null;
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
            return null;
        }
    }
}
