package com.teukgeupjeonsa.backend.collector.crawler;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.dto.MndOpenApiListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MndOpenApiListCrawlerService {

    private static final String[] ROW_SELECTORS = {
            "table tbody tr",
            ".board_list tbody tr",
            ".tbl_list tbody tr"
    };

    private static final Pattern UNIT_PATTERN = Pattern.compile("((제\\d+부대|[가-힣A-Za-z]+훈련소|[가-힣A-Za-z]+부대))");

    private final MealCollectorProperties properties;

    public List<MndOpenApiListItem> crawlMealCandidates() {
        List<MndOpenApiListItem> result = new ArrayList<>();

        for (int page = 1; page <= properties.getMaxPages(); page++) {
            String listUrl = buildListUrl(page);
            try {
                Document doc = Jsoup.connect(listUrl)
                        .timeout(properties.getTimeoutMillis())
                        .get();

                List<MndOpenApiListItem> pageRows = parseRows(doc, listUrl);
                result.addAll(pageRows);
                log.info("목록 페이지 파싱 완료 page={}, found={}", page, pageRows.size());
            } catch (IOException e) {
                log.warn("목록 페이지 파싱 실패 url={}", listUrl, e);
            }
        }

        log.info("목록 수집 완료 totalFound={}", result.size());
        return result;
    }

    private String buildListUrl(int page) {
        return properties.getOpenapiListUrlTemplate().replace("{page}", String.valueOf(page));
    }

    private List<MndOpenApiListItem> parseRows(Document doc, String pageUrl) {
        Elements rows = selectRows(doc);
        List<MndOpenApiListItem> items = new ArrayList<>();

        for (Element row : rows) {
            Element link = row.selectFirst("a[href]");
            if (link == null) {
                continue;
            }

            String title = link.text().trim();
            if (!isMealRelated(title)) {
                continue;
            }

            String detailUrl = link.absUrl("href");
            if (detailUrl == null || detailUrl.isBlank()) {
                detailUrl = pageUrl;
            }

            String rowText = row.text();
            String provider = parseProvider(row);
            LocalDate updatedAt = parseUpdatedAt(rowText);

            items.add(new MndOpenApiListItem(
                    title,
                    detailUrl,
                    provider,
                    updatedAt,
                    extractUnitNameCandidate(title)
            ));
        }

        return items;
    }

    private Elements selectRows(Document doc) {
        for (String selector : ROW_SELECTORS) {
            Elements rows = doc.select(selector);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return new Elements();
    }

    private boolean isMealRelated(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        if (!normalized.contains("식단")) {
            return false;
        }

        return normalized.contains("부대") || normalized.contains("훈련소") || normalized.contains("병영 표준 식단");
    }

    private String parseProvider(Element row) {
        Element tdWithProvider = row.selectFirst("td:matchesOwn(국방부|육군|해군|공군|해병)");
        if (tdWithProvider != null) {
            return tdWithProvider.text().trim();
        }

        Elements tds = row.select("td");
        if (tds.size() >= 3) {
            return tds.get(2).text().trim();
        }
        return null;
    }

    private LocalDate parseUpdatedAt(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("(20\\d{2}[.-/]\\d{1,2}[.-/]\\d{1,2})").matcher(text);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1).replace('.', '-').replace('/', '-');
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String extractUnitNameCandidate(String title) {
        Matcher matcher = UNIT_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return sanitizeUnitName(title);
    }

    private String sanitizeUnitName(String title) {
        return title
                .replaceAll("식단\\s*정보_?일별", "")
                .replaceAll("식단\\s*정보", "")
                .replaceAll("병영\\s*표준\\s*식단", "")
                .replaceAll("식단정보", "")
                .replaceAll("식단", "")
                .trim();
    }
}
