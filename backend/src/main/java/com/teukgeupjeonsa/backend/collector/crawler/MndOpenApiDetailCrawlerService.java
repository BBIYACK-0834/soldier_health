package com.teukgeupjeonsa.backend.collector.crawler;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.dto.MndOpenApiDetailInfo;
import com.teukgeupjeonsa.backend.collector.dto.MndOpenApiListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MndOpenApiDetailCrawlerService {

    private static final Pattern SERVICE_PATTERN = Pattern.compile("(DS_TB_MNDT_[A-Z0-9_]+)");
    private static final Pattern OPENAPI_URL_PATTERN = Pattern.compile("https?://openapi\\.mnd\\.go\\.kr");

    private final MealCollectorProperties properties;

    public Optional<MndOpenApiDetailInfo> parseDetail(MndOpenApiListItem item) {
        try {
            Document doc = Jsoup.connect(item.detailUrl())
                    .timeout(properties.getTimeoutMillis())
                    .get();

            String pageText = doc.text();
            String service = findService(doc.html(), pageText);
            if (service == null || service.isBlank()) {
                log.warn("상세 페이지 SERVICE 추출 실패 detailUrl={}, title={}", item.detailUrl(), item.title());
                return Optional.empty();
            }

            String openApiBaseUrl = findOpenApiBaseUrl(doc.html(), pageText);
            String unitName = sanitizeUnitName(item.unitNameCandidate() != null ? item.unitNameCandidate() : item.title());

            MndOpenApiDetailInfo info = new MndOpenApiDetailInfo(
                    unitName,
                    service,
                    openApiBaseUrl,
                    item.detailUrl(),
                    item.provider(),
                    item.updatedAt(),
                    item.title()
            );

            log.info("상세 페이지 파싱 성공 unitName={}, serviceName={}", info.unitName(), info.serviceName());
            return Optional.of(info);
        } catch (IOException e) {
            log.warn("상세 페이지 요청 실패 detailUrl={}", item.detailUrl(), e);
            return Optional.empty();
        }
    }

    private String findService(String html, String text) {
        Matcher serviceMatcher = SERVICE_PATTERN.matcher(text);
        if (serviceMatcher.find()) {
            return serviceMatcher.group(1);
        }

        serviceMatcher = Pattern.compile("SERVICE[^A-Z0-9]*(DS_TB_MNDT_[A-Z0-9_]+)").matcher(html);
        if (serviceMatcher.find()) {
            return serviceMatcher.group(1);
        }
        return null;
    }

    private String findOpenApiBaseUrl(String html, String text) {
        Matcher matcher = OPENAPI_URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        matcher = OPENAPI_URL_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group();
        }

        return "https://openapi.mnd.go.kr";
    }

    private String sanitizeUnitName(String rawTitle) {
        return rawTitle
                .replaceAll("식단\\s*정보_?일별", "")
                .replaceAll("식단\\s*정보", "")
                .replaceAll("병영\\s*표준\\s*식단", "")
                .replaceAll("식단정보", "")
                .replaceAll("식단", "")
                .trim();
    }
}
