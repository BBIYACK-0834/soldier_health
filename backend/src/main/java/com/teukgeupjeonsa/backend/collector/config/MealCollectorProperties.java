package com.teukgeupjeonsa.backend.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "meal-collector")
public class MealCollectorProperties {

    /**
     * 목록 페이지 URL 템플릿. {page} 치환자를 사용한다.
     */
    private String openapiListUrlTemplate =
            "https://www.data.mil.kr/openapi/list.do?apiType=OPEN_API&search=%EC%8B%9D%EB%8B%A8&page={page}";

    private int maxPages = 4;

    private int timeoutMillis = 10000;
}
