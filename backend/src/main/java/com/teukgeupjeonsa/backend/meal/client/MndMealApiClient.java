package com.teukgeupjeonsa.backend.meal.client;

import com.teukgeupjeonsa.backend.meal.config.MndApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MndMealApiClient {

    private final MndApiProperties properties;
    private final WebClient.Builder webClientBuilder;

    public Map<String, Object> fetchRows(String serviceCode, int startIndex, int endIndex) {
        String key = properties.getKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("mnd.api.key 값이 설정되지 않았습니다.");
        }

        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment(key, "json", serviceCode, String.valueOf(startIndex), String.valueOf(endIndex), "")
                .build(true)
                .toUriString();

        log.info("MND OpenAPI 요청 serviceCode={}, startIndex={}, endIndex={}, uri={}", serviceCode, startIndex, endIndex, uri);

        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, "soldier-health-backend/1.0")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            int keyCount = response == null ? 0 : response.size();
            log.info("MND OpenAPI 응답 serviceCode={}, topLevelKeyCount={}", serviceCode, keyCount);
            return response;
        } catch (Exception e) {
            log.error("MND OpenAPI 호출 실패 serviceCode={}, startIndex={}, endIndex={}", serviceCode, startIndex, endIndex, e);
            throw new IllegalStateException("국방부 OpenAPI 호출 실패 serviceCode=" + serviceCode, e);
        }
    }
}
