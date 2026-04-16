package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.dto.MealCollectionSummary;
import com.teukgeupjeonsa.backend.collector.dto.MealPersistResult;
import com.teukgeupjeonsa.backend.collector.openapi.MndOpenApiClient;
import com.teukgeupjeonsa.backend.collector.parser.MndMealResponseParser;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealOpenApiCollectionService {

    private static final String SOURCE_NAME = "mnd-openapi";

    private final MndOpenApiClient openApiClient;
    private final MndMealResponseParser responseParser;
    private final MealMenuRepository mealMenuRepository;
    private final MealCollectorServiceCodeResolver serviceCodeResolver;

    @Transactional
    public MealCollectionSummary collectAllFromFixedServices() {
        List<String> serviceCodes = serviceCodeResolver.resolveFixedServiceCodes();
        log.info("고정 서비스 목록 수집 시작 totalServices={}", serviceCodes.size());

        int apiSucceeded = 0;
        int apiFailed = 0;
        int insertedRows = 0;
        int updatedRows = 0;
        List<String> failedServices = new ArrayList<>();

        for (String serviceCode : serviceCodes) {
            try {
                Map<String, Object> response = openApiClient.fetchMeals(serviceCode);
                List<MndMealResponseParser.ParsedMealRow> parsedRows = responseParser.parseRows(serviceCode, response);
                MealPersistResult persistResult = persistMealRows(parsedRows);

                insertedRows += persistResult.inserted();
                updatedRows += persistResult.updated();
                apiSucceeded++;

<<<<<<< HEAD
                log.info("OpenAPI 응답 성공 serviceCode={}, parsedRows={}", serviceCode, parsedRows.size());
                log.info("서비스 적재 완료 serviceCode={}, inserted={}, updated={}, skipped={}",
                        serviceCode, persistResult.inserted(), persistResult.updated(), persistResult.skipped());
            } catch (Exception e) {
                apiFailed++;
                failedServices.add(serviceCode);
                log.warn("OpenAPI 응답 실패 serviceCode={}", serviceCode);
                log.error("서비스 수집 실패 serviceCode={}", serviceCode, e);
=======
                log.info("OpenAPI 응답 성공 unitName={}, serviceName={}, parsedRows={}",
                        detailInfo.unitName(), detailInfo.serviceName(), parsedRows.size());
                log.info("부대 적재 완료 unitName={}, serviceName={}, inserted={}, updated={}, skipped={}",
                        detailInfo.unitName(), detailInfo.serviceName(), persistResult.inserted(), persistResult.updated(), persistResult.skipped());
            } catch (Exception e) {
                apiFailed++;
                failedUnits.add(detailInfo.unitName() + "(" + detailInfo.serviceName() + ")");
                log.warn("OpenAPI 응답 실패 unitName={}, serviceName={}", detailInfo.unitName(), detailInfo.serviceName());
                log.error("부대 수집 실패 unitName={}, serviceName={}", detailInfo.unitName(), detailInfo.serviceName(), e);
>>>>>>> origin/main
            }
        }

        log.info("수집 요약 totalServices={}, apiSucceeded={}, apiFailed={}, insertedRows={}, updatedRows={}",
                serviceCodes.size(), apiSucceeded, apiFailed, insertedRows, updatedRows);

        return MealCollectionSummary.builder()
                .success(apiFailed == 0)
                .totalFound(serviceCodes.size())
                .detailParsed(serviceCodes.size())
                .apiSucceeded(apiSucceeded)
                .apiFailed(apiFailed)
                .insertedRows(insertedRows)
                .updatedRows(updatedRows)
                .skippedUnits(List.of())
                .failedUnits(failedServices)
                .build();
    }

    @Transactional
    public MealCollectionSummary collectByServiceName(String rawServiceName) {
        String serviceCode;
        try {
            serviceCode = serviceCodeResolver.resolveSingle(rawServiceName);
        } catch (IllegalArgumentException e) {
            log.warn("서비스 코드 해석 실패 input={}", rawServiceName);
            return MealCollectionSummary.builder()
                    .success(false)
                    .totalFound(1)
                    .detailParsed(1)
                    .apiSucceeded(0)
                    .apiFailed(1)
                    .insertedRows(0)
                    .updatedRows(0)
                    .skippedUnits(List.of())
                    .failedUnits(List.of(rawServiceName))
                    .build();
        }

        try {
            Map<String, Object> response = openApiClient.fetchMeals(serviceCode);
            List<MndMealResponseParser.ParsedMealRow> parsedRows = responseParser.parseRows(serviceCode, response);
            MealPersistResult persistResult = persistMealRows(parsedRows);

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
            log.error("단건 수집 실패 serviceCode={}", serviceCode, e);
            return MealCollectionSummary.builder()
                    .success(false)
                    .totalFound(1)
                    .detailParsed(1)
                    .apiSucceeded(0)
                    .apiFailed(1)
                    .insertedRows(0)
                    .updatedRows(0)
                    .skippedUnits(List.of())
                    .failedUnits(List.of(serviceCode))
                    .build();
        }
    }

    private MealPersistResult persistMealRows(List<MndMealResponseParser.ParsedMealRow> rows) {
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
<<<<<<< HEAD
            entity.setSourceName(SOURCE_NAME);
=======
            entity.setSourceName(sourceName);
>>>>>>> origin/main
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

        return new MealPersistResult(inserted, updated, skipped);
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
}
