package com.teukgeupjeonsa.backend.meal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "mnd.api")
public class MndApiProperties {

    private String baseUrl = "https://openapi.mnd.go.kr";
    private String key;
    private List<String> serviceCodes = new ArrayList<>();
}
