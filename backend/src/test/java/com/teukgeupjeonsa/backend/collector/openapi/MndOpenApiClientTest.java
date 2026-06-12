package com.teukgeupjeonsa.backend.collector.openapi;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.config.PublicMealApiProperties;
import io.netty.channel.ConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class MndOpenApiClientTest {

    private final MealCollectorProperties collectorProperties = new MealCollectorProperties();
    private final MndOpenApiClient client = new MndOpenApiClient(
            new PublicMealApiProperties(),
            collectorProperties,
            WebClient.builder()
    );

    @Test
    void requestTimeoutUsesConfiguredCollectorTimeout() {
        collectorProperties.setTimeoutMillis(90_000);

        assertThat(client.requestTimeout()).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void retryableIncludesConnectionAndTimeoutFailures() {
        assertThat(client.isRetryable(new TimeoutException("request timeout"))).isTrue();
        assertThat(client.isRetryable(new ConnectTimeoutException("connect timeout"))).isTrue();
        assertThat(client.isRetryable(new ConnectException("connection refused"))).isTrue();
        assertThat(client.isRetryable(new UnknownHostException("openapi.mnd.go.kr"))).isTrue();
    }

    @Test
    void nonNetworkRuntimeExceptionIsNotRetryable() {
        assertThat(client.isRetryable(new IllegalArgumentException("bad request"))).isFalse();
    }
}
