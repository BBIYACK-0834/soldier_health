package com.teukgeupjeonsa.backend.meal.scheduler;

import com.teukgeupjeonsa.backend.meal.service.MealImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mnd.meal-import", name = "scheduler-enabled", havingValue = "true")
public class MealImportScheduler {

    private final MealImportService mealImportService;

    @Scheduled(cron = "${mnd.meal-import.scheduler-cron:0 0 5,17 * * *}")
    public void runImport() {
        MealImportService.ImportSummary summary = mealImportService.importAll();
        log.info("식단 import 스케줄 실행 완료 inserted={}, updated={}, skipped={}, failures={}",
                summary.inserted(), summary.updated(), summary.skipped(), summary.failures().size());
    }
}
