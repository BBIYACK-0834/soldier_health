package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.dto.MealCollectionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCollectionScheduler {

    private final MealOpenApiCollectionService mealOpenApiCollectionService;

    @Scheduled(
            cron = "${meal-collector.auto-collect-cron:0 0 3 1 * *}",
            zone = "${meal-collector.auto-collect-zone:Asia/Seoul}"
    )
    public void collectMonthlyMeals() {
        log.info("[MealCollectScheduler] 월간 식단 수집 스케줄 시작");
        try {
            MealCollectionSummary summary = mealOpenApiCollectionService.collectAllFromFixedServices();
            log.info(
                    "[MealCollectScheduler] 월간 식단 수집 종료 success={}, insertedRows={}, updatedRows={}, apiSucceeded={}, apiFailed={}",
                    summary.success(),
                    summary.insertedRows(),
                    summary.updatedRows(),
                    summary.apiSucceeded(),
                    summary.apiFailed()
            );
        } catch (Exception e) {
            log.error("[MealCollectScheduler] 월간 식단 수집 실패 - 서버는 계속 실행됩니다.", e);
        }
    }
}
