package com.teukgeupjeonsa.backend.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "meal-collector")
public class MealCollectorProperties {

    private String baseDomain = "https://www.data.go.kr";
    private String searchUrlTemplate = "https://www.data.go.kr/tcs/dss/selectDataSetList.do?keyword=%EA%B5%AD%EB%B0%A9%EB%B6%80%20%EC%8B%9D%EB%8B%A8&pageIndex=%d";
    private int startPage = 1;
    private int maxPages = 1;
    private int timeoutMillis = 10000;
    private String includeTitleRegex = "^국방부_.*식단정보.*$";
    private List<String> denyKeywords = new ArrayList<>(List.of("PX", "표준단어목록", "국간사후보", "사전", "학보"));
    private List<String> detailPageAllowlist = new ArrayList<>();
    private String downloadDirectory = "./data/meal-csv";
    private int skipRedownloadHours = 24 * 365;
    private boolean schedulerEnabled = false;
    private String schedulerCron = "0 0 4 * * *";

    public Path ensureDownloadDirectory() {
        try {
            Path path = Path.of(downloadDirectory);
            Files.createDirectories(path);
            return path;
        } catch (Exception e) {
            throw new IllegalStateException("CSV 다운로드 디렉터리 생성 실패: " + downloadDirectory, e);
        }
    }
}
