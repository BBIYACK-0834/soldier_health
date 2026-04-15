package com.teukgeupjeonsa.backend.mealcrawler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class HttpRetryClient {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public HttpResponse<String> getText(String url, Map<String, String> headers, int retryCount) {
        return execute(url, headers, retryCount, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<byte[]> getBytes(String url, Map<String, String> headers, int retryCount) {
        return execute(url, headers, retryCount, HttpResponse.BodyHandlers.ofByteArray());
    }

    private <T> HttpResponse<T> execute(String url,
                                        Map<String, String> headers,
                                        int retryCount,
                                        HttpResponse.BodyHandler<T> bodyHandler) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= Math.max(3, retryCount); attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", USER_AGENT);

                if (headers != null) {
                    headers.forEach(builder::header);
                }

                HttpResponse<T> response = httpClient.send(builder.build(), bodyHandler);
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response;
                }
                log.warn("HTTP 요청 실패 status={} attempt={} url={}", response.statusCode(), attempt, url);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("HTTP 요청 예외 attempt={} url={} message={}", attempt, url, e.getMessage());
                lastException = new RuntimeException(e);
            }

            sleepBackoff(attempt);
        }

        throw new IllegalStateException("HTTP 요청 3회 이상 실패: " + url, lastException);
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(300L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
