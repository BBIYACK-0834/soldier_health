package com.teukgeupjeonsa.backend.collector.openapi;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.config.PublicMealApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MndOpenApiClient {

    private static final int RETRY_COUNT = 2;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final int BODY_LOG_LIMIT = 300;

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

        log.info("OpenAPI 요청 serviceName={}, rows={}, timeoutMs={}, url={}",
                serviceName,
                apiProperties.getRows(),
                collectorProperties.getTimeoutMillis(),
                maskKey(requestUrl, serviceKey));

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(requestUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(response -> handleResponse(serviceName, response.statusCode(), response.bodyToMono(String.class)))
                    .timeout(Duration.ofMillis(collectorProperties.getTimeoutMillis()))
                    .retryWhen(Retry.fixedDelay(RETRY_COUNT, RETRY_DELAY)
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn(
                                    "OpenAPI 재시도 serviceName={}, attempt={}/{}, reason={}",
                                    serviceName,
                                    signal.totalRetries() + 1,
                                    RETRY_COUNT,
                                    simplifyMessage(signal.failure())
                            )))
                    .block();
        } catch (Exception e) {
            Throwable rootCause = rootCauseOf(e);
            if (rootCause instanceof TimeoutException) {
                log.error("OpenAPI timeout serviceName={}, timeoutMs={}", serviceName, collectorProperties.getTimeoutMillis(), e);
                throw new IllegalStateException("OpenAPI timeout: " + serviceName, e);
            }
            if (rootCause instanceof ConnectException || rootCause instanceof UnknownHostException) {
                log.error("OpenAPI connection error serviceName={}", serviceName, e);
                throw new IllegalStateException("OpenAPI connection error: " + serviceName, e);
            }
            log.error("OpenAPI 호출 실패 serviceName={}", serviceName, e);
            throw new IllegalStateException("OpenAPI 호출 실패: " + serviceName, e);
        }
    }

    private Mono<Map<String, Object>> handleResponse(String serviceName, HttpStatusCode statusCode, Mono<String> bodyMono) {
        return bodyMono.defaultIfEmpty("")
                .flatMap(body -> {
                    String bodySnippet = truncate(body);
                    if (!statusCode.is2xxSuccessful()) {
                        log.warn("OpenAPI 비정상 응답 serviceName={}, status={}, bodySnippet={}",
                                serviceName,
                                statusCode.value(),
                                bodySnippet);
                        return Mono.error(new OpenApiHttpException(statusCode.value()));
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("OpenAPI 응답 serviceName={}, status={}, bodySnippet={}",
                                serviceName,
                                statusCode.value(),
                                bodySnippet);
                    }

                    return Mono.justOrEmpty(body)
                            .filter(value -> !value.isBlank())
                            .flatMap(value -> Mono.just(value)
                                    .map(v -> new org.springframework.boot.json.JacksonJsonParser().parseMap(v)))
                            .switchIfEmpty(Mono.error(new IllegalStateException("OpenAPI 응답 body가 비어 있습니다.")));
                });
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable rootCause = rootCauseOf(throwable);
        if (rootCause instanceof TimeoutException || rootCause instanceof ConnectException || rootCause instanceof UnknownHostException) {
            return true;
        }

        if (rootCause instanceof OpenApiHttpException ex) {
            return ex.statusCode >= 500;
        }

        return false;
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String truncate(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        if (body.length() <= BODY_LOG_LIMIT) {
            return body;
        }
        return body.substring(0, BODY_LOG_LIMIT) + "...";
    }

    private String simplifyMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }

    private String maskKey(String url, String key) {
        return url.replace(key, "***MASKED***");
    }

    private static final class OpenApiHttpException extends RuntimeException {
        private final int statusCode;

        private OpenApiHttpException(int statusCode) {
            super("OpenAPI HTTP status=" + statusCode);
            this.statusCode = statusCode;
        }
    }
}
