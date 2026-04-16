package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.crawler.MndOpenApiDetailCrawlerService;
import com.teukgeupjeonsa.backend.collector.crawler.MndOpenApiListCrawlerService;
import com.teukgeupjeonsa.backend.collector.dto.*;
import com.teukgeupjeonsa.backend.collector.entity.UnitApiSource;
import com.teukgeupjeonsa.backend.collector.openapi.MndOpenApiClient;
import com.teukgeupjeonsa.backend.collector.parser.MndMealResponseParser;
import com.teukgeupjeonsa.backend.collector.repository.UnitApiSourceRepository;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealOpenApiCollectionService {

    private static final String SOURCE_NAME = "mnd-openapi";

    private final MndOpenApiListCrawlerService listCrawlerService;
    private final MndOpenApiDetailCrawlerService detailCrawlerService;
    private final MndOpenApiClient openApiClient;
    private final MndMealResponseParser responseParser;
    private final MealMenuRepository mealMenuRepository;
    private final UnitApiSourceRepository unitApiSourceRepository;
    private final MilitaryUnitRepository militaryUnitRepository;

    @Transactional
    public MealCollectionSummary collectAllFromList() {
        List<MndOpenApiListItem> listItems = listCrawlerService.crawlMealCandidates();
        int detailParsed = 0;
        int apiSucceeded = 0;
        int apiFailed = 0;
        int insertedRows = 0;
        int updatedRows = 0;
        List<String> skippedUnits = new ArrayList<>();
        List<String> failedUnits = new ArrayList<>();

        for (MndOpenApiListItem item : listItems) {
            Optional<MndOpenApiDetailInfo> detailOptional = detailCrawlerService.parseDetail(item);
            if (detailOptional.isEmpty()) {
                skippedUnits.add(item.title());
                continue;
            }
            detailParsed++;

            MndOpenApiDetailInfo detailInfo = detailOptional.get();
            upsertUnitApiSource(detailInfo);

            try {
                Map<String, Object> response = openApiClient.fetchMeals(detailInfo.serviceName(), detailInfo.openApiBaseUrl());
                List<MndMealResponseParser.ParsedMealRow> parsedRows = responseParser.parseRows(detailInfo, response);
                MealPersistResult persistResult = persistMealRows(detailInfo, parsedRows);
                insertedRows += persistResult.inserted();
                updatedRows += persistResult.updated();
                apiSucceeded++;

                log.info("OpenAPI 응답 성공 unitName={}, serviceName={}, parsedRows={}",
                        detailInfo.unitName(), detailInfo.serviceName(), parsedRows.size());
                log.info("부대 적재 완료 unitName={}, serviceName={}, inserted={}, updated={}, skipped={}",
                        detailInfo.unitName(), detailInfo.serviceName(), persistResult.inserted(), persistResult.updated(), persistResult.skipped());
            } catch (Exception e) {
                apiFailed++;
                failedUnits.add(detailInfo.unitName() + "(" + detailInfo.serviceName() + ")");
                log.warn("OpenAPI 응답 실패 unitName={}, serviceName={}", detailInfo.unitName(), detailInfo.serviceName());
                log.error("부대 수집 실패 unitName={}, serviceName={}", detailInfo.unitName(), detailInfo.serviceName(), e);
            }
        }

        log.info("수집 요약 totalFound={}, detailParsed={}, apiSucceeded={}, apiFailed={}, insertedRows={}, updatedRows={}",
                listItems.size(), detailParsed, apiSucceeded, apiFailed, insertedRows, updatedRows);

        return MealCollectionSummary.builder()
                .success(apiFailed == 0)
                .totalFound(listItems.size())
                .detailParsed(detailParsed)
                .apiSucceeded(apiSucceeded)
                .apiFailed(apiFailed)
                .insertedRows(insertedRows)
                .updatedRows(updatedRows)
                .skippedUnits(skippedUnits)
                .failedUnits(failedUnits)
                .build();
    }

    @Transactional
    public MealCollectionSummary collectByUnitName(String unitName) {
        Optional<UnitApiSource> sourceOptional = unitApiSourceRepository.findTopByUnitNameIgnoreCaseAndActiveTrue(unitName);
        if (sourceOptional.isEmpty()) {
            log.warn("UnitApiSource를 찾을 수 없음 unitName={}", unitName);
            return MealCollectionSummary.builder()
                    .success(false)
                    .totalFound(0)
                    .detailParsed(0)
                    .apiSucceeded(0)
                    .apiFailed(1)
                    .insertedRows(0)
                    .updatedRows(0)
                    .skippedUnits(List.of(unitName))
                    .failedUnits(List.of(unitName))
                    .build();
        }

        return collectFromSource(sourceOptional.get());
    }

    @Transactional
    public MealCollectionSummary collectByServiceName(String serviceName) {
        Optional<UnitApiSource> sourceOptional = unitApiSourceRepository.findByServiceName(serviceName);
        if (sourceOptional.isEmpty()) {
            log.warn("UnitApiSource를 찾을 수 없음 serviceName={}", serviceName);
            return MealCollectionSummary.builder()
                    .success(false)
                    .totalFound(0)
                    .detailParsed(0)
                    .apiSucceeded(0)
                    .apiFailed(1)
                    .insertedRows(0)
                    .updatedRows(0)
                    .skippedUnits(List.of(serviceName))
                    .failedUnits(List.of(serviceName))
                    .build();
        }

        return collectFromSource(sourceOptional.get());
    }

    private MealCollectionSummary collectFromSource(UnitApiSource source) {
        MndOpenApiDetailInfo detailInfo = new MndOpenApiDetailInfo(
                source.getUnitName(),
                source.getServiceName(),
                source.getOpenApiBaseUrl(),
                source.getDetailUrl(),
                source.getProvider(),
                source.getSourceUpdatedAt(),
                source.getUnitName()
        );

        try {
            Map<String, Object> response = openApiClient.fetchMeals(detailInfo.serviceName(), detailInfo.openApiBaseUrl());
            List<MndMealResponseParser.ParsedMealRow> parsedRows = responseParser.parseRows(detailInfo, response);
            MealPersistResult persistResult = persistMealRows(detailInfo, parsedRows);

            return MealCollectionSummary.builder()
                    .success(true)
                    .totalFound(1)
                    .detailParsed(1)
                    .apiSucceeded(1)
                    .apiFailed(0)
                    .insertedRows(persistResult.inserted())
                    .updatedRows(persistResult.updated())
                    .skippedUnits(List.of())
                    .failedUnits(List.of())
                    .build();
        } catch (Exception e) {
            log.error("단건 수집 실패 unitName={}, serviceName={}", source.getUnitName(), source.getServiceName(), e);
            return MealCollectionSummary.builder()
                    .success(false)
                    .totalFound(1)
                    .detailParsed(1)
                    .apiSucceeded(0)
                    .apiFailed(1)
                    .insertedRows(0)
                    .updatedRows(0)
                    .skippedUnits(List.of())
                    .failedUnits(List.of(source.getServiceName()))
                    .build();
        }
    }

    private void upsertUnitApiSource(MndOpenApiDetailInfo detailInfo) {
        UnitApiSource source = unitApiSourceRepository.findByServiceName(detailInfo.serviceName())
                .orElseGet(UnitApiSource::new);

        source.setUnitName(detailInfo.unitName());
        source.setDetailUrl(detailInfo.detailUrl());
        source.setServiceName(detailInfo.serviceName());
        source.setOpenApiBaseUrl(detailInfo.openApiBaseUrl());
        source.setProvider(detailInfo.provider());
        source.setSourceUpdatedAt(detailInfo.updatedAt());
        source.setActive(true);

        unitApiSourceRepository.save(source);
    }

    private MealPersistResult persistMealRows(MndOpenApiDetailInfo detailInfo, List<MndMealResponseParser.ParsedMealRow> rows) {
        if (rows.isEmpty()) {
            return MealPersistResult.empty();
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        String sourceName = SOURCE_NAME + ":" + detailInfo.unitName();
        for (MndMealResponseParser.ParsedMealRow row : rows) {
            if (row.mealDate() == null) {
                skipped++;
                continue;
            }

            MealMenu entity = mealMenuRepository.findBySourceNameAndMealDate(sourceName, row.mealDate())
                    .or(() -> mealMenuRepository.findByServiceCodeAndMealDate(row.serviceName(), row.mealDate()))
                    .orElseGet(MealMenu::new);

            boolean isInsert = entity.getId() == null;
            entity.setServiceCode(row.serviceName());
            entity.setSourceName(sourceName);
            entity.setMealDate(row.mealDate());
            entity.setBreakfast(row.breakfastRaw());
            entity.setLunch(row.lunchRaw());
            entity.setDinner(row.dinnerRaw());
            entity.setBreakfastKcal(row.breakfastKcal());
            entity.setLunchKcal(row.lunchKcal());
            entity.setDinnerKcal(row.dinnerKcal());
            entity.setTotalKcal(sum(row.breakfastKcal(), row.lunchKcal(), row.dinnerKcal()));
            mealMenuRepository.save(entity);

            if (isInsert) {
                inserted++;
            } else {
                updated++;
            }
        }

        updateUnitDataSourceKey(detailInfo.unitName(), detailInfo.serviceName());
        return new MealPersistResult(inserted, updated, skipped);
    }

    private void updateUnitDataSourceKey(String unitName, String serviceName) {
        List<MilitaryUnit> units = militaryUnitRepository.findAll();
        String normalizedTarget = normalize(unitName);
        for (MilitaryUnit unit : units) {
            if (normalize(unit.getUnitName()).equals(normalizedTarget)) {
                if (!Objects.equals(unit.getDataSourceKey(), serviceName)) {
                    unit.setDataSourceKey(serviceName);
                    militaryUnitRepository.save(unit);
                    log.info("unit dataSourceKey 업데이트 unitName={}, serviceName={}", unit.getUnitName(), serviceName);
                }
                return;
            }
        }
    }

    private int sum(Integer... values) {
        int total = 0;
        boolean has = false;
        for (Integer value : values) {
            if (value != null) {
                total += value;
                has = true;
            }
        }
        return has ? total : 0;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
