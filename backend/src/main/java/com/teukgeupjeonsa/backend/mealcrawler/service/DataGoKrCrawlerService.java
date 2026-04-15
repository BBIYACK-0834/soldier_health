package com.teukgeupjeonsa.backend.mealcrawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGoKrCrawlerService {

    private static final String SEARCH_URL = "https://www.data.go.kr/tcs/dss/selectDataSetList.do?dType=FILE&keyword=국방부 식단";

    private final HttpRetryClient httpRetryClient;

    public List<String> collectDetailPageUrls() {
        log.info("[1/4] data.go.kr 검색 페이지 요청: {}", SEARCH_URL);

        String html = httpRetryClient.getText(SEARCH_URL, Map.of("Referer", "https://www.data.go.kr"), 3).body();
        Document document = Jsoup.parse(html, SEARCH_URL);

        Set<String> detailUrls = new LinkedHashSet<>();

        for (Element anchor : document.select("a[href]")) {
            String href = anchor.attr("href");
            String absolute = toAbsoluteUrl(SEARCH_URL, href);
            if (absolute == null || absolute.isBlank()) {
                continue;
            }

            if (absolute.matches("https://www\\.data\\.go\\.kr/data/\\d+/fileData\\.do.*")) {
                detailUrls.add(absolute);
            }
        }

        log.info("검색 결과에서 상세페이지 URL {}건 수집", detailUrls.size());
        return detailUrls.stream().toList();
    }

    private String toAbsoluteUrl(String baseUrl, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        if (raw.startsWith("//")) {
            return "https:" + raw;
        }
        try {
            return URI.create(baseUrl).resolve(raw).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
