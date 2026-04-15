package com.teukgeupjeonsa.backend.mealcrawler.service;

import com.teukgeupjeonsa.backend.mealcrawler.entity.MealMenu;
import com.teukgeupjeonsa.backend.mealcrawler.repository.MealMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealMenuCrawlOrchestrator {

    private final DataGoKrCrawlerService dataGoKrCrawlerService;
    private final DataGoKrDetailService dataGoKrDetailService;
    private final MndDataDownloadService mndDataDownloadService;
    private final CsvParsingService csvParsingService;
    private final MealMenuRepository mealMenuRepository;

    @Transactional
    public void crawlAllMeals() {
        List<String> detailUrls = dataGoKrCrawlerService.collectDetailPageUrls();
        log.info("수집 대상 상세페이지 {}건", detailUrls.size());

        int upsertCount = 0;
        int failCount = 0;

        for (String detailUrl : detailUrls) {
            try {
                String infId = dataGoKrDetailService.extractInfId(detailUrl).orElseThrow(
                        () -> new IllegalStateException("infId 추출 실패")
                );

                byte[] csvBytes = mndDataDownloadService.downloadCsv(infId).orElseThrow(
                        () -> new IllegalStateException("CSV 다운로드 실패")
                );

                List<MealMenu> parsedRows = csvParsingService.parse(infId, "국방부", csvBytes);
                for (MealMenu row : parsedRows) {
                    upsert(row);
                    upsertCount++;
                }

                log.info("데이터셋 처리 완료 detailUrl={}, infId={}, rows={}", detailUrl, infId, parsedRows.size());
            } catch (Exception e) {
                failCount++;
                log.warn("데이터셋 처리 실패 detailUrl={}", detailUrl, e);
            }
        }

        log.info("[4/4] crawlAllMeals 완료 upsertCount={}, failCount={}", upsertCount, failCount);
    }

    private void upsert(MealMenu incoming) {
        mealMenuRepository.findByInfIdAndMealDate(incoming.getInfId(), incoming.getMealDate())
                .map(existing -> {
                    existing.setSourceName(incoming.getSourceName());
                    existing.setBreakfast(incoming.getBreakfast());
                    existing.setLunch(incoming.getLunch());
                    existing.setDinner(incoming.getDinner());
                    existing.setBreakfastKcal(incoming.getBreakfastKcal());
                    existing.setLunchKcal(incoming.getLunchKcal());
                    existing.setDinnerKcal(incoming.getDinnerKcal());
                    existing.setTotalKcal(incoming.getTotalKcal());
                    return mealMenuRepository.save(existing);
                })
                .orElseGet(() -> mealMenuRepository.save(incoming));
    }
}
