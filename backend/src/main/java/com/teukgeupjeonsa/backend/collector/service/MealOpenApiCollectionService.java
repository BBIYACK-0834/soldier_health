package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import com.teukgeupjeonsa.backend.collector.config.PublicMealApiProperties;
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
    private final PublicMealApiProperties apiProperties;
    private final MealCollectorProperties collectorProperties;

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
                MealPersistResult persistResult = collectServiceByPages(serviceCode);

                insertedRows += persistResult.inserted();
                updatedRows += persistResult.updated();
                apiSucceeded++;

                log.info("서비스 적재 완료 serviceCode={}, inserted={}, updated={}, skipped={}",
                        serviceCode,
                        persistResult.inserted(),
                        persistResult.updated(),
                        persistResult.skipped());
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
            MealPersistResult persistResult = collectServiceByPages(serviceCode);

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

    private MealPersistResult collectServiceByPages(String serviceCode) {
        // 한 번 API 요청 시 가져올 개수
        int pageSize = Math.max(1, apiProperties.getRows());

        // 전체 수집 가능한 최대 범위 (초기값)
        int totalLimit = Math.max(pageSize, collectorProperties.getPageSize());

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        boolean unitUpserted = false;
        Integer knownTotalCount = null; // 💡 API가 알려주는 진짜 전체 개수를 저장할 변수

        log.info("서비스 페이지 수집 시작 serviceCode={}, 설정된 초기 totalLimit={}, pageSize={}", 
                 serviceCode, totalLimit, pageSize);

        for (int startRow = 1; startRow <= totalLimit; startRow += pageSize) {
            int endRow = Math.min(startRow + pageSize - 1, totalLimit);

            log.info("OpenAPI 호출 serviceCode={}, range={}-{}", serviceCode, startRow, endRow);

            Map<String, Object> response = openApiClient.fetchMeals(serviceCode, startRow, endRow);

            // 💡 핵심 1: 첫 응답에서 진짜 전체 데이터 개수(list_total_count)를 알아내어 목표치(totalLimit)를 수정합니다.
            if (knownTotalCount == null) {
                knownTotalCount = extractTotalCount(response, serviceCode);
                if (knownTotalCount != null && knownTotalCount > 0) {
                    totalLimit = knownTotalCount;
                    // 변경된 totalLimit에 맞춰서 이번 요청의 endRow도 재조정 (안전장치)
                    endRow = Math.min(startRow + pageSize - 1, totalLimit);
                    log.info("💡 API 전체 데이터 개수 감지됨: {}개! 이 개수 끝까지 수집을 진행합니다.", totalLimit);
                }
            }

            List<MndMealResponseParser.ParsedMealRow> parsedRows = responseParser.parseRows(serviceCode, response);

            log.info("OpenAPI 페이지 파싱 완료 serviceCode={}, range={}-{}, 파싱된 유효 데이터={}", 
                     serviceCode, startRow, endRow, parsedRows.size());

            // 💡 핵심 2: 빈 페이지(함정)를 감지하더라도 break(종료)하지 않고 continue(무시하고 직진) 합니다!
            if (parsedRows.isEmpty()) {
                log.info("⚠️ 쓰레기(빈) 데이터 구간 감지 - 무시하고 다음 페이지로 넘어갑니다. (range={}-{})", startRow, endRow);
                
                // 만약 빈 데이터가 하필 맨 마지막 페이지였다면 여기서 종료
                if (endRow >= totalLimit) {
                    log.info("목표한 전체 데이터 범위 탐색 완료 (마지막 빈 데이터 처리)");
                    break;
                }
                continue; // 다음 번호로!
            }

            // 부대 정보 최초 1회 저장
            if (!unitUpserted) {
                upsertUnitFromRows(serviceCode, parsedRows);
                unitUpserted = true;
            }

            MealPersistResult pageResult = persistMealRows(parsedRows);

            inserted += pageResult.inserted();
            updated += pageResult.updated();
            skipped += pageResult.skipped();

            // 💡 핵심 3: 파싱된 개수가 적다고 종료하지 않습니다! 
            // API가 최초에 알려준 전체 개수(totalLimit)를 모두 순회했을 때만 진정으로 루프를 종료합니다.
            if (endRow >= totalLimit) {
                log.info("✅ 모든 데이터 범위 탐색 완료 - 수집 종료 serviceCode={}, 탐색한 최종 번호={}", serviceCode, endRow);
                break;
            }
        }

        log.info("서비스 수집 최종 결과 serviceCode={}, inserted={}, updated={}, skipped={}",
                serviceCode, inserted, updated, skipped);

        return new MealPersistResult(inserted, updated, skipped);
    }

    // 💡 추가된 헬퍼 메서드: API JSON 응답에서 list_total_count를 안전하게 추출합니다.
    private Integer extractTotalCount(Map<String, Object> response, String serviceCode) {
        try {
            if (response != null && response.containsKey(serviceCode)) {
                Object serviceRoot = response.get(serviceCode);
                if (serviceRoot instanceof Map<?, ?> rootMap) {
                    Object listTotalCount = rootMap.get("list_total_count");
                    if (listTotalCount instanceof Number) {
                        return ((Number) listTotalCount).intValue();
                    } else if (listTotalCount instanceof String) {
                        return Integer.parseInt((String) listTotalCount);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("list_total_count 추출 실패 serviceCode={}", serviceCode, e);
        }
        return null;
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
                entity.setTotalKcal(sum(
                        entity.getBreakfastKcal(),
                        entity.getLunchKcal(),
                        entity.getDinnerKcal()
                ));
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