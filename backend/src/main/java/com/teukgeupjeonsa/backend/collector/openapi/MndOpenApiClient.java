package com.teukgeupjeonsa.backend.collector.openapi;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.config.PublicMealApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MndOpenApiClient {

    private final PublicMealApiProperties apiProperties;
    private final MealCollectorProperties collectorProperties;
    private final WebClient.Builder webClientBuilder;

    public Map<String, Object> fetchMeals(String serviceName) {
        String serviceKey = apiProperties.getServiceKey();
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("public-meal.api.service-key 값이 비어 있습니다.");
        }

        String requestUrl = UriComponentsBuilder.fromUriString(apiProperties.getBaseUrl())
                .pathSegment(
                        serviceKey,
                        apiProperties.getType(),
                        serviceName,
                        "1",
                        String.valueOf(apiProperties.getRows()),
                        ""
                )
                .build(true)
                .toUriString();

        log.info("OpenAPI 요청 serviceName={}, url={}", serviceName, maskKey(requestUrl, serviceKey));

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(requestUrl)
                    .header(HttpHeaders.USER_AGENT, "soldier-health-backend/collector")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(collectorProperties.getTimeoutMillis()))
                    .block();
        } catch (Exception e) {
            log.error("OpenAPI 호출 실패 serviceName={}, url={}", serviceName, maskKey(requestUrl, serviceKey), e);
            throw new IllegalStateException("OpenAPI 호출 실패: " + serviceName, e);
        }
    }

    private String maskKey(String url, String key) {
        return url.replace(key, "***MASKED***");
    }
}
