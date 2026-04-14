package com.teukgeupjeonsa.backend.collector.scheduler;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.service.MealCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meal-collector", name = "scheduler-enabled", havingValue = "true")
public class MealCollectionScheduler {

    private final MealCollectionService mealCollectionService;
    private final MealCollectorProperties properties;

    @Scheduled(cron = "${meal-collector.scheduler-cron:0 0 4 * * *}")
    public void scheduledCollect() {
        log.info("스케줄 수집 시작 cron={}", properties.getSchedulerCron());
        mealCollectionService.collectAndDownload();
    }
}
