package com.teukgeupjeonsa.backend.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "public-meal.api")
public class PublicMealApiProperties {

    private String baseUrl = "https://openapi.mnd.go.kr";
    private String serviceKey;
    private int rows = 200;
    private String type = "json";
}
