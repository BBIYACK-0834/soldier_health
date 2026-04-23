package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.dto.MealCollectionSummary;
import com.teukgeupjeonsa.backend.collector.dto.MealPersistResult;
import com.teukgeupjeonsa.backend.collector.openapi.MndOpenApiClient;
import com.teukgeupjeonsa.backend.collector.parser.MndMealResponseParser;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
import com.teukgeupjeonsa.backend.user.BranchType;
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
    private final MilitaryUnitRepository militaryUnitRepository;

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
                upsertUnitFromRows(serviceCode, parsedRows);
                MealPersistResult persistResult = persistMealRows(parsedRows);

                insertedRows += persistResult.inserted();
                updatedRows += persistResult.updated();
                apiSucceeded++;

                log.info("OpenAPI 응답 성공 serviceCode={}, parsedRows={}", serviceCode, parsedRows.size());
                log.info("서비스 적재 완료 serviceCode={}, inserted={}, updated={}, skipped={}",
                        serviceCode, persistResult.inserted(), persistResult.updated(), persistResult.skipped());
            } catch (Exception e) {
                apiFailed++;
                failedServices.add(serviceCode);
                log.warn("OpenAPI 응답 실패 serviceCode={}", serviceCode);
                log.error("서비스 수집 실패 serviceCode={}", serviceCode, e);
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
            upsertUnitFromRows(serviceCode, parsedRows);
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

        for (MndMealResponseParser.ParsedMealRow row : rows) {
            if (row.mealDate() == null) {
                skipped++;
                continue;
            }

            MealMenu entity = mealMenuRepository.findByServiceCodeAndMealDate(row.serviceName(), row.mealDate())
                    .orElseGet(MealMenu::new);

            boolean isInsert = entity.getId() == null;
            entity.setServiceCode(row.serviceName());
            entity.setSourceName(SOURCE_NAME);
            entity.setMealDate(row.mealDate());
            entity.setBreakfast(mergeMealText(entity.getBreakfast(), row.breakfastRaw()));
            entity.setLunch(mergeMealText(entity.getLunch(), row.lunchRaw()));
            entity.setDinner(mergeMealText(entity.getDinner(), row.dinnerRaw()));
            entity.setBreakfastKcal(mergeKcal(entity.getBreakfastKcal(), row.breakfastKcal()));
            entity.setLunchKcal(mergeKcal(entity.getLunchKcal(), row.lunchKcal()));
            entity.setDinnerKcal(mergeKcal(entity.getDinnerKcal(), row.dinnerKcal()));

            Integer rowTotal = row.totalKcal();
            if (rowTotal != null && rowTotal > 0) {
                entity.setTotalKcal(rowTotal);
            } else {
                entity.setTotalKcal(sum(entity.getBreakfastKcal(), entity.getLunchKcal(), entity.getDinnerKcal()));
            }

            mealMenuRepository.save(entity);

            if (isInsert) {
                inserted++;
            } else {
                updated++;
            }
        }

        return new MealPersistResult(inserted, updated, skipped);
    }

    private void upsertUnitFromRows(String serviceCode, List<MndMealResponseParser.ParsedMealRow> rows) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return;
        }

        String normalizedServiceCode = serviceCode.trim();
        String resolvedUnitName = rows.stream()
                .map(MndMealResponseParser.ParsedMealRow::unitName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseGet(() -> "부대 " + simplifyServiceCode(normalizedServiceCode));
        String resolvedRegionName = rows.stream()
                .map(MndMealResponseParser.ParsedMealRow::regionName)
                .filter(region -> region != null && !region.isBlank())
                .findFirst()
                .orElse("미상");

        MilitaryUnit unit = militaryUnitRepository.findByDataSourceKeyIgnoreCase(normalizedServiceCode)
                .orElseGet(() -> MilitaryUnit.builder()
                        .unitCode("AUTO-" + simplifyServiceCode(normalizedServiceCode))
                        .build());

        unit.setDataSourceKey(normalizedServiceCode);
        unit.setUnitName(resolvedUnitName);
        unit.setRegionName(resolvedRegionName);
        if (unit.getBranchType() == null || unit.getBranchType() == BranchType.ETC) {
            unit.setBranchType(inferBranchType(resolvedUnitName));
        }
        if (unit.getUnitCode() == null || unit.getUnitCode().isBlank()) {
            unit.setUnitCode("AUTO-" + simplifyServiceCode(normalizedServiceCode));
        }

        militaryUnitRepository.save(unit);
    }

    private BranchType inferBranchType(String unitName) {
        if (unitName == null || unitName.isBlank()) {
            return BranchType.ETC;
        }
        if (unitName.contains("육군")) {
            return BranchType.ARMY;
        }
        if (unitName.contains("해군")) {
            return BranchType.NAVY;
        }
        if (unitName.contains("공군")) {
            return BranchType.AIR_FORCE;
        }
        if (unitName.contains("해병")) {
            return BranchType.MARINES;
        }
        return BranchType.ETC;
    }

    private String simplifyServiceCode(String serviceCode) {
        if (serviceCode == null) {
            return "UNKNOWN";
        }
        return serviceCode
                .replace("DS_TB_MNDT_DATEBYMLSVC_", "")
                .replaceAll("[^A-Z0-9_-]", "_");
    }

    private String mergeMealText(String current, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return incoming.trim();
        }

        String normalizedCurrent = current.trim();
        String normalizedIncoming = incoming.trim();
        if (normalizedCurrent.equals(normalizedIncoming)) {
            return normalizedCurrent;
        }
        if (normalizedCurrent.contains(normalizedIncoming)) {
            return normalizedCurrent;
        }
        return normalizedCurrent + ", " + normalizedIncoming;
    }

    private Integer mergeKcal(Integer current, Integer incoming) {
        if (incoming == null) {
            return current;
        }
        if (current == null) {
            return incoming;
        }
        return current + incoming;
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
