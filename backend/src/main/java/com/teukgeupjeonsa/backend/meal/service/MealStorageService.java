package com.teukgeupjeonsa.backend.meal.service;

import com.teukgeupjeonsa.backend.collector.dto.CrawledDataset;
import com.teukgeupjeonsa.backend.collector.dto.ParsedMealRow;
import com.teukgeupjeonsa.backend.meal.entity.DatasetSource;
import com.teukgeupjeonsa.backend.meal.entity.MilitaryMeal;
import com.teukgeupjeonsa.backend.meal.repository.DatasetSourceRepository;
import com.teukgeupjeonsa.backend.meal.repository.MilitaryMealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealStorageService {

    private final DatasetSourceRepository datasetSourceRepository;
    private final MilitaryMealRepository militaryMealRepository;

    @Transactional
    public DatasetSource upsertSource(CrawledDataset dataset, String downloadUrl) {
        DatasetSource source = datasetSourceRepository.findBySourceUrl(dataset.sourceUrl())
                .orElseGet(DatasetSource::new);
        source.setSourceTitle(dataset.title());
        source.setSourceUrl(dataset.sourceUrl());
        source.setDownloadUrl(downloadUrl);
        source.setProvider(dataset.provider());
        source.setFormat("CSV");
        source.setActive(true);
        source.setLastCollectedAt(LocalDateTime.now());
        return datasetSourceRepository.save(source);
    }

    @Transactional
    public SaveStat saveMeals(DatasetSource source, List<ParsedMealRow> rows) {
        int saved = 0;
        int duplicate = 0;
        for (ParsedMealRow row : rows) {
            MilitaryMeal meal = militaryMealRepository.findBySourceAndUnitNameAndMealDate(source, row.unitName(), row.mealDate())
                    .orElseGet(MilitaryMeal::new);
            boolean isNew = meal.getId() == null;
            String oldHash = meal.getRawRowHash();

            meal.setSource(source);
            meal.setBranch(row.branch());
            meal.setUnitName(row.unitName());
            meal.setMealDate(row.mealDate());
            meal.setBreakfast(row.breakfast());
            meal.setLunch(row.lunch());
            meal.setDinner(row.dinner());
            meal.setRawRowHash(row.rawRowHash());
            militaryMealRepository.save(meal);

            if (!isNew && row.rawRowHash().equals(oldHash)) {
                duplicate++;
            } else {
                saved++;
            }
        }
        log.info("저장 완료 sourceId={}, saved={}, duplicate={}", source.getId(), saved, duplicate);
        return new SaveStat(saved, duplicate);
    }

    public record SaveStat(int savedCount, int duplicateCount) {}
}
