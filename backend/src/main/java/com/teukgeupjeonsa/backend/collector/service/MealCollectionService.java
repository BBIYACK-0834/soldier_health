package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.crawler.MealDatasetCrawlerService;
import com.teukgeupjeonsa.backend.collector.download.MealDatasetDownloadService;
import com.teukgeupjeonsa.backend.collector.dto.CollectionResult;
import com.teukgeupjeonsa.backend.collector.dto.CrawledDataset;
import com.teukgeupjeonsa.backend.collector.dto.ParsedMealRow;
import com.teukgeupjeonsa.backend.collector.parser.MealCsvParseService;
import com.teukgeupjeonsa.backend.meal.entity.DatasetSource;
import com.teukgeupjeonsa.backend.meal.service.MealStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCollectionService {

    private final MealDatasetCrawlerService crawlerService;
    private final MealDatasetDownloadService downloadService;
    private final MealCsvParseService parseService;
    private final MealStorageService storageService;

    public CollectionResult collectAll() {
        int downloaded = 0;
        int parsed = 0;
        int saved = 0;
        int duplicates = 0;
        int failed = 0;

        List<CrawledDataset> explored = crawlerService.crawlSearchPages();
        List<CrawledDataset> filtered = crawlerService.filterMealDatasets(explored);

        log.info("필터 통과 제목={}", filtered.stream().map(CrawledDataset::title).toList());

        for (CrawledDataset item : filtered) {
            try {
                String downloadUrl = crawlerService.findCsvDownloadUrl(item.sourceUrl());
                if (downloadUrl == null || !downloadUrl.toLowerCase().contains("csv")) {
                    log.warn("CSV 다운로드 링크 없음 title={}, sourceUrl={}", item.title(), item.sourceUrl());
                    failed++;
                    continue;
                }
                Path file = downloadService.downloadCsv(downloadUrl);
                downloaded++;

                List<ParsedMealRow> rows = parseService.parse(file, item.title());
                parsed += rows.size();

                DatasetSource source = storageService.upsertSource(item, downloadUrl);
                MealStorageService.SaveStat stat = storageService.saveMeals(source, rows);
                saved += stat.savedCount();
                duplicates += stat.duplicateCount();
            } catch (Exception e) {
                failed++;
                log.warn("데이터셋 처리 실패 title={}, url={}", item.title(), item.sourceUrl(), e);
            }
        }

        CollectionResult result = CollectionResult.builder()
                .exploredCount(explored.size())
                .filteredCount(filtered.size())
                .downloadedCount(downloaded)
                .parsedCount(parsed)
                .savedCount(saved)
                .duplicateCount(duplicates)
                .failedCount(failed)
                .build();

        log.info("최종 수집 요약={}", result);
        return result;
    }
}
