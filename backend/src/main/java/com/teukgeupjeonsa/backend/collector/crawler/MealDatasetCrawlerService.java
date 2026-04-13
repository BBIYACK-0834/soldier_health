package com.teukgeupjeonsa.backend.collector.crawler;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.dto.CrawledDataset;
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

    private final MealCollectorProperties properties;

    public List<CrawledDataset> crawlSearchPages() {
        List<CrawledDataset> all = new ArrayList<>();
        for (int page = properties.getStartPage(); page < properties.getStartPage() + properties.getMaxPages(); page++) {
            String url = String.format(properties.getSearchUrlTemplate(), page);
            try {
                Document doc = Jsoup.connect(url).timeout(properties.getTimeoutMillis()).get();
                all.addAll(parseList(doc));
            } catch (Exception e) {
                log.warn("검색 페이지 크롤링 실패 page={}, url={}", page, url, e);
            }
        }
        log.info("검색 결과 탐색 건수={}", all.size());
        return all;
    }

    public List<CrawledDataset> filterMealDatasets(List<CrawledDataset> items) {
        Pattern includePattern = Pattern.compile(properties.getIncludeTitleRegex());
        return items.stream()
                .filter(i -> includePattern.matcher(i.title().trim()).matches())
                .filter(i -> properties.getDenyKeywords().stream().noneMatch(k -> i.title().contains(k)))
                .toList();
    }

    public String findCsvDownloadUrl(String detailPageUrl) {
        try {
            Document doc = Jsoup.connect(detailPageUrl).timeout(properties.getTimeoutMillis()).get();
            Elements links = doc.select(PublicDataPortalSelectors.DETAIL_DOWNLOAD_LINKS);
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (href == null || href.isBlank()) {
                    href = toAbsolute(detailPageUrl, link.attr("href"));
                }
                String label = (link.text() + " " + link.attr("title") + " " + href).toLowerCase();
                if (label.contains("csv") || label.contains("download") || label.contains("다운로드") || href.toLowerCase().endsWith(".csv")) {
                    return href;
                }
            }
        } catch (Exception e) {
            log.warn("상세 페이지 접근 실패 url={}", detailPageUrl, e);
        }
        return null;
    }

    private List<CrawledDataset> parseList(Document doc) {
        List<CrawledDataset> result = new ArrayList<>();
        Elements rows = doc.select(PublicDataPortalSelectors.DATASET_ITEM);
        for (Element row : rows) {
            Element titleLink = row.selectFirst(PublicDataPortalSelectors.TITLE_LINK);
            if (titleLink == null) {
                continue;
            }
            String title = titleLink.text().trim();
            String sourceUrl = titleLink.attr("abs:href");
            String provider = row.select(PublicDataPortalSelectors.PROVIDER).text();
            String description = row.select(PublicDataPortalSelectors.DESCRIPTION).text();
            String format = row.text().contains("CSV") ? "CSV" : null;
            result.add(CrawledDataset.builder()
                    .title(title)
                    .sourceUrl(sourceUrl)
                    .provider(provider)
                    .description(description)
                    .format(format)
                    .build());
        }
        return result;
    }

    private String toAbsolute(String baseUrl, String maybeRelative) {
        if (maybeRelative == null || maybeRelative.isBlank()) return maybeRelative;
        if (maybeRelative.startsWith("http://") || maybeRelative.startsWith("https://")) return maybeRelative;
        if (maybeRelative.startsWith("//")) return "https:" + maybeRelative;
        try {
            URI base = URI.create(baseUrl);
            return base.resolve(maybeRelative).toString();
        } catch (Exception e) {
            return properties.getBaseDomain() + (maybeRelative.startsWith("/") ? maybeRelative : "/" + maybeRelative);
        }
    }
}
