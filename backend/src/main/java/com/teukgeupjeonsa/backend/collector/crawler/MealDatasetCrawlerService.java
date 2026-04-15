package com.teukgeupjeonsa.backend.collector.crawler;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.dto.CollectedDatasetItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealDatasetCrawlerService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MealCollectorBot/1.0)";

    private final MealCollectorProperties properties;

    public List<CollectedDatasetItem> collectCandidates() {
        return applyFilters(crawlSearchPages());
    }

    public List<CollectedDatasetItem> crawlSearchPages() {
        List<CollectedDatasetItem> allItems = new ArrayList<>();

        int start = properties.getStartPage();
        int end = start + Math.max(properties.getMaxPages(), 1) - 1;

        for (int page = start; page <= end; page++) {
            String searchUrl = String.format(properties.getSearchUrlTemplate(), page);
            try {
                log.info("검색 결과 페이지 요청 page={}, url={}", page, searchUrl);
                Document document = Jsoup.connect(searchUrl)
                        .userAgent(USER_AGENT)
                        .timeout(properties.getTimeoutMillis())
                        .get();

                allItems.addAll(parseSearchPage(document, searchUrl));
            } catch (Exception e) {
                log.warn("검색 결과 페이지 파싱 실패 page={}, url={}", page, searchUrl, e);
            }
        }

        List<CollectedDatasetItem> deduplicated = deduplicate(allItems);
        log.info("검색 결과 전체 건수={} (필터 전)", deduplicated.size());

        if (deduplicated.isEmpty()) {
            List<CollectedDatasetItem> fallbackItems = buildAllowlistFallbackItems();
            if (!fallbackItems.isEmpty()) {
                log.warn("검색 결과가 0건이어서 detail-page-allowlist를 후보 목록으로 사용합니다. allowlistCount={}", fallbackItems.size());
                return fallbackItems;
            }
        }

        return deduplicated;
    }

    public List<CollectedDatasetItem> applyFilters(List<CollectedDatasetItem> allItems) {
        Pattern includePattern = Pattern.compile(properties.getIncludeTitleRegex());
        List<CollectedDatasetItem> matchedItems = new ArrayList<>();

        for (CollectedDatasetItem item : allItems) {
            if (!includePattern.matcher(item.title().trim()).matches()) {
                continue;
            }
            boolean denied = properties.getDenyKeywords().stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .anyMatch(keyword -> item.title().contains(keyword));
            if (denied) {
                log.info("deny-keyword로 제외 title={}", item.title());
                continue;
            }
            matchedItems.add(item);
        }

        log.info("제목 정규식/deny-keyword 필터 통과 건수={}", matchedItems.size());
        return matchedItems;
    }

    private List<CollectedDatasetItem> parseSearchPage(Document document, String searchUrl) {
        log.info("검색 페이지 디버그 title='{}', totalLinks={}", document.title(), document.select("a[href]").size());

        String[] selectors = {
                "a[href*='fileData.do']",
                "a[href*='/data/']",
                "a[href*='selectDataSetDetail']",
                ".data-set a[href]",
                ".result-list a[href]",
                ".search-result a[href]"
        };

        Elements candidateLinks = new Elements();
        for (String selector : selectors) {
            Elements selected = document.select(selector);
            candidateLinks.addAll(selected);
            log.info("검색 페이지 디버그 selector='{}' matches={}", selector, selected.size());
        }
        log.info("검색 페이지 디버그 candidate selector 총 매칭 수={}", candidateLinks.size());

        List<CollectedDatasetItem> preferred = new ArrayList<>();
        List<CollectedDatasetItem> secondary = new ArrayList<>();

        for (Element link : candidateLinks) {
            String title = link.text() == null ? "" : link.text().trim();
            if (title.isBlank()) {
                continue;
            }

            String href = link.attr("abs:href");
            if (href == null || href.isBlank()) {
                href = toAbsoluteUrl(searchUrl, link.attr("href"));
            }
            if (href == null || href.isBlank()) {
                continue;
            }

            CollectedDatasetItem item = new CollectedDatasetItem(
                    title,
                    href,
                    extractProvider(nearestContainer(link)),
                    extractModifiedAt(nearestContainer(link))
            );

            if (isPreferredDetailPageUrl(href)) {
                preferred.add(item);
            } else {
                secondary.add(item);
            }
        }

        List<CollectedDatasetItem> selectedItems = !preferred.isEmpty() ? preferred : secondary;
        List<CollectedDatasetItem> deduplicated = deduplicate(selectedItems);
        log.info("검색 페이지 디버그 preferred={}, secondary={}, 최종 deduplicated={}", preferred.size(), secondary.size(), deduplicated.size());
        return deduplicated;
    }

    private boolean isPreferredDetailPageUrl(String href) {
        return href.contains("/data/") || href.contains("fileData.do");
    }

    private List<CollectedDatasetItem> buildAllowlistFallbackItems() {
        List<CollectedDatasetItem> fallbackItems = new ArrayList<>();

        for (String url : properties.getDetailPageAllowlist()) {
            if (url == null || url.isBlank()) {
                continue;
            }

            String trimmedUrl = url.trim();
            if (!isPreferredDetailPageUrl(trimmedUrl)) {
                continue;
            }

            fallbackItems.add(new CollectedDatasetItem(
                    "국방부_식단정보_allowlist",
                    trimmedUrl,
                    "",
                    ""
            ));
        }

        return deduplicate(fallbackItems);
    }

    private Element nearestContainer(Element element) {
        Element li = element.closest("li");
        if (li != null) {
            return li;
        }
        Element article = element.closest("article");
        if (article != null) {
            return article;
        }
        Element div = element.closest("div");
        return div != null ? div : element;
    }

    private String extractProvider(Element container) {
        if (container == null) {
            return "";
        }
        Element providerEl = container.selectFirst(".org, .provider, .result-info, .data-info, [class*='기관']");
        return providerEl != null ? providerEl.text().trim() : "";
    }

    private String extractModifiedAt(Element container) {
        if (container == null) {
            return "";
        }
        Element modifiedEl = container.selectFirst(".update, .modified, .date, [class*='수정']");
        if (modifiedEl != null) {
            return modifiedEl.text().trim();
        }
        String text = container.text();
        int idx = text.indexOf("수정");
        if (idx >= 0) {
            int end = Math.min(text.length(), idx + 20);
            return text.substring(idx, end).trim();
        }
        return "";
    }

    private List<CollectedDatasetItem> deduplicate(List<CollectedDatasetItem> rawItems) {
        List<CollectedDatasetItem> deduplicated = new ArrayList<>();
        for (CollectedDatasetItem item : rawItems) {
            boolean exists = deduplicated.stream()
                    .anyMatch(existing -> existing.title().equals(item.title())
                            && existing.detailUrl().equals(item.detailUrl()));
            if (!exists) {
                deduplicated.add(item);
            }
        }
        return deduplicated;
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
            URI base = URI.create(baseUrl);
            return base.resolve(maybeRelative).toString();
        } catch (Exception e) {
            if (maybeRelative.startsWith("/")) {
                return properties.getBaseDomain() + maybeRelative;
            }
            return properties.getBaseDomain() + "/" + maybeRelative;
        }
    }
}
