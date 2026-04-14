package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.crawler.MealDatasetCrawlerService;
import com.teukgeupjeonsa.backend.collector.download.MealDatasetDownloadService;
import com.teukgeupjeonsa.backend.collector.dto.CollectedDatasetItem;
import com.teukgeupjeonsa.backend.collector.dto.DownloadResult;
import com.teukgeupjeonsa.backend.collector.dto.MealCollectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCollectionService {

    private final MealDatasetCrawlerService crawlerService;
    private final MealDatasetDownloadService downloadService;

    public MealCollectionResponse collectAndDownload() {
        List<CollectedDatasetItem> allFound = crawlerService.crawlSearchPages();
        List<CollectedDatasetItem> matched = crawlerService.applyFilters(allFound);

        int downloaded = 0;
        int failed = 0;
        List<String> downloadedFiles = new ArrayList<>();
        List<String> skippedTitles = new ArrayList<>();
        List<String> failedTitles = new ArrayList<>();

        for (CollectedDatasetItem item : matched) {
            try {
                DownloadResult result = downloadService.downloadCsv(item);
                if (result.downloaded()) {
                    downloaded++;
                    if (result.savedPath() != null && !result.savedPath().isBlank()) {
                        downloadedFiles.add(result.savedPath());
                    }
                    continue;
                }

                if (result.skipped()) {
                    skippedTitles.add(item.title());
                    continue;
                }

                failed++;
                failedTitles.add(item.title());
            } catch (Exception e) {
                failed++;
                failedTitles.add(item.title());
                log.warn("데이터셋 다운로드 처리 실패 title={}, detailUrl={}", item.title(), item.detailUrl(), e);
            }
        }

        MealCollectionResponse response = new MealCollectionResponse(
                failed == 0,
                allFound.size(),
                matched.size(),
                downloaded,
                failed,
                downloadedFiles,
                skippedTitles,
                failedTitles
        );

        log.info("CSV 수집 파이프라인 완료 totalFound={}, matched={}, downloaded={}, failed={}, skipped={}",
                response.totalFound(), response.matched(), response.downloaded(), response.failed(), response.skippedTitles().size());

        return response;
    }
}
