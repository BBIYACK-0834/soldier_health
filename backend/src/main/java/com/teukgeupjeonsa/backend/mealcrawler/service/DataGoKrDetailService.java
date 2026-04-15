package com.teukgeupjeonsa.backend.mealcrawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGoKrDetailService {

    private static final Pattern INF_ID_PATTERN = Pattern.compile("(OA-\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHEET_URL_PATTERN = Pattern.compile(
            "https://opendata\\.mnd\\.go\\.kr/openinf/sheetview2\\.jsp\\?infId=(OA-\\d+)", Pattern.CASE_INSENSITIVE);

    private final HttpRetryClient httpRetryClient;

    public Optional<String> extractInfId(String detailUrl) {
        try {
            log.info("[2/4] 상세페이지에서 infId 추출 시작: {}", detailUrl);
            String html = httpRetryClient.getText(detailUrl, Map.of("Referer", "https://www.data.go.kr"), 3).body();

            Document document = Jsoup.parse(html, detailUrl);

            for (Element element : document.select("a[href], iframe[src], script[src], [data-url], [onclick]")) {
                String merged = String.join(" ",
                        element.attr("href"),
                        element.attr("src"),
                        element.attr("data-url"),
                        element.attr("onclick"),
                        element.outerHtml());

                Optional<String> fromSheetUrl = extractByPattern(merged, SHEET_URL_PATTERN, 1);
                if (fromSheetUrl.isPresent()) {
                    log.info("상세페이지 infId 추출 성공: {} -> {}", detailUrl, fromSheetUrl.get());
                    return fromSheetUrl;
                }

                Optional<String> fromInfId = extractByPattern(merged, INF_ID_PATTERN, 1);
                if (fromInfId.isPresent()) {
                    log.info("상세페이지 infId 추출(정규식 fallback) 성공: {} -> {}", detailUrl, fromInfId.get());
                    return fromInfId;
                }
            }

            Optional<String> fromWholeHtml = extractByPattern(html, SHEET_URL_PATTERN, 1)
                    .or(() -> extractByPattern(html, INF_ID_PATTERN, 1));

            fromWholeHtml.ifPresent(infId -> log.info("상세페이지 infId 전체 HTML 탐색 성공: {} -> {}", detailUrl, infId));
            if (fromWholeHtml.isEmpty()) {
                log.warn("상세페이지 infId 추출 실패: {}", detailUrl);
            }
            return fromWholeHtml;
        } catch (Exception e) {
            log.warn("상세페이지 infId 추출 중 예외: {}", detailUrl, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractByPattern(String input, Pattern pattern, int groupIndex) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(groupIndex)).map(String::toUpperCase);
        }
        return Optional.empty();
    }
}
