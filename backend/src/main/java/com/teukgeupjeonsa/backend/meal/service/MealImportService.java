package com.teukgeupjeonsa.backend.meal.service;

import com.teukgeupjeonsa.backend.meal.client.MndMealApiClient;
import com.teukgeupjeonsa.backend.meal.config.MndApiProperties;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealImportService {

    private static final int PAGE_SIZE = 1000;
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private static final List<String> SOURCE_NAME_KEYS = List.of("부대명", "기관명", "소속", "sourceName", "unitName");
    private static final List<String> DATE_KEYS = List.of("날짜", "일자", "급식일자", "식단일자", "급식일", "mealDate", "date", "일시");
    private static final List<String> BREAKFAST_KEYS = List.of("조식", "조식메뉴", "breakfast", "breakfastMenu", "조식식단");
    private static final List<String> LUNCH_KEYS = List.of("중식", "중식메뉴", "lunch", "lunchMenu", "중식식단");
    private static final List<String> DINNER_KEYS = List.of("석식", "석식메뉴", "dinner", "dinnerMenu", "석식식단");
    private static final List<String> BREAKFAST_KCAL_KEYS = List.of("조식열량", "조식칼로리", "조식kcal", "breakfastKcal", "조식(열량)");
    private static final List<String> LUNCH_KCAL_KEYS = List.of("중식열량", "중식칼로리", "중식kcal", "lunchKcal", "중식(열량)");
    private static final List<String> DINNER_KCAL_KEYS = List.of("석식열량", "석식칼로리", "석식kcal", "dinnerKcal", "석식(열량)");
    private static final List<String> TOTAL_KCAL_KEYS = List.of("총열량", "총칼로리", "총kcal", "totalKcal", "일일총열량");

    private final MndApiProperties properties;
    private final MndMealApiClient mndMealApiClient;
    private final MealMenuRepository mealMenuRepository;

    @Transactional
    public ImportSummary importAll() {
        List<String> serviceCodes = properties.getServiceCodes();
        if (serviceCodes == null || serviceCodes.isEmpty()) {
            log.warn("mnd.api.service-codes 값이 비어있어 import를 건너뜁니다.");
            return new ImportSummary(0, 0, 0, List.of());
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();

        for (String serviceCode : serviceCodes) {
            if (serviceCode == null || serviceCode.isBlank()) {
                continue;
            }
            try {
                ServiceCodeStat stat = importServiceCode(serviceCode.trim());
                inserted += stat.inserted();
                updated += stat.updated();
                skipped += stat.skipped();
            } catch (Exception e) {
                log.error("serviceCode import 실패 serviceCode={}", serviceCode, e);
                failures.add(serviceCode + ": " + e.getMessage());
            }
        }

        return new ImportSummary(inserted, updated, skipped, failures);
    }

    private ServiceCodeStat importServiceCode(String serviceCode) {
        int start = 1;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        while (true) {
            int end = start + PAGE_SIZE - 1;
            Map<String, Object> response = mndMealApiClient.fetchRows(serviceCode, start, end);
            List<Map<String, Object>> rows = extractRows(response, serviceCode);
            if (rows.isEmpty()) {
                if (start == 1) {
                    log.warn("row 데이터 없음 serviceCode={}", serviceCode);
                }
                break;
            }

            for (Map<String, Object> row : rows) {
                ParseResult parsed = parseRow(serviceCode, row);
                if (!parsed.valid()) {
                    skipped++;
                    continue;
                }

                MealMenu mealMenu = mealMenuRepository.findByServiceCodeAndMealDate(serviceCode, parsed.mealDate())
                        .orElseGet(MealMenu::new);

                boolean isInsert = mealMenu.getId() == null;
                mealMenu.setServiceCode(serviceCode);
                mealMenu.setSourceName(parsed.sourceName());
                mealMenu.setMealDate(parsed.mealDate());
                mealMenu.setBreakfast(parsed.breakfast());
                mealMenu.setLunch(parsed.lunch());
                mealMenu.setDinner(parsed.dinner());
                mealMenu.setBreakfastKcal(parsed.breakfastKcal());
                mealMenu.setLunchKcal(parsed.lunchKcal());
                mealMenu.setDinnerKcal(parsed.dinnerKcal());
                mealMenu.setTotalKcal(parsed.totalKcal());
                mealMenuRepository.save(mealMenu);

                if (isInsert) {
                    inserted++;
                } else {
                    updated++;
                }
            }

            if (rows.size() < PAGE_SIZE) {
                break;
            }
            start += PAGE_SIZE;
        }

        log.info("serviceCode import 완료 serviceCode={}, inserted={}, updated={}, skipped={}", serviceCode, inserted, updated, skipped);
        return new ServiceCodeStat(inserted, updated, skipped);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> response, String serviceCode) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }

        Object serviceRoot = response.get(serviceCode);
        if (!(serviceRoot instanceof List<?> serviceArray) || serviceArray.size() < 2) {
            return List.of();
        }

        Object rowContainer = serviceArray.get(1);
        if (!(rowContainer instanceof Map<?, ?> rowMap)) {
            return List.of();
        }

        Object rows = rowMap.get("row");
        if (!(rows instanceof List<?> rowList)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rowList) {
            if (item instanceof Map<?, ?> itemMap) {
                result.add((Map<String, Object>) itemMap);
            }
        }
        return result;
    }

    private ParseResult parseRow(String serviceCode, Map<String, Object> row) {
        String dateRaw = firstText(row, DATE_KEYS);
        LocalDate mealDate = parseDate(dateRaw);
        if (mealDate == null) {
            log.warn("날짜 파싱 실패로 row skip serviceCode={}, dateRaw={}", serviceCode, dateRaw);
            return ParseResult.invalid();
        }

        Integer breakfastKcal = parseInteger(firstText(row, BREAKFAST_KCAL_KEYS));
        Integer lunchKcal = parseInteger(firstText(row, LUNCH_KCAL_KEYS));
        Integer dinnerKcal = parseInteger(firstText(row, DINNER_KCAL_KEYS));
        Integer totalKcal = parseInteger(firstText(row, TOTAL_KCAL_KEYS));

        String sourceName = Optional.ofNullable(blankToNull(firstText(row, SOURCE_NAME_KEYS)))
                .orElse(serviceCode);

        return ParseResult.valid(
                mealDate,
                sourceName,
                blankToNull(firstText(row, BREAKFAST_KEYS)),
                blankToNull(firstText(row, LUNCH_KEYS)),
                blankToNull(firstText(row, DINNER_KEYS)),
                breakfastKcal,
                lunchKcal,
                dinnerKcal,
                totalKcal
        );
    }

    private String firstText(Map<String, Object> row, List<String> aliases) {
        for (String alias : aliases) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                if (normalizeKey(key).equals(normalizeKey(alias))) {
                    Object value = entry.getValue();
                    if (value != null) {
                        String text = String.valueOf(value).trim();
                        if (!text.isEmpty()) {
                            return text;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9가-힣]", "");
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        List<String> candidates = Arrays.asList(
                raw.trim(),
                raw.replaceAll("\\.", "-").replaceAll("/", "-").trim(),
                raw.replaceAll("[^0-9]", "").trim()
        );

        for (String candidate : candidates) {
            try {
                if (candidate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(candidate);
                }
                if (candidate.matches("\\d{8}")) {
                    return LocalDate.parse(candidate, BASIC_DATE);
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9-]", "").trim();
        if (digits.isBlank() || "-".equals(digits)) {
            return null;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record ServiceCodeStat(int inserted, int updated, int skipped) {
    }

    private record ParseResult(
            boolean valid,
            LocalDate mealDate,
            String sourceName,
            String breakfast,
            String lunch,
            String dinner,
            Integer breakfastKcal,
            Integer lunchKcal,
            Integer dinnerKcal,
            Integer totalKcal
    ) {
        static ParseResult invalid() {
            return new ParseResult(false, null, null, null, null, null, null, null, null, null);
        }

        static ParseResult valid(LocalDate mealDate,
                                 String sourceName,
                                 String breakfast,
                                 String lunch,
                                 String dinner,
                                 Integer breakfastKcal,
                                 Integer lunchKcal,
                                 Integer dinnerKcal,
                                 Integer totalKcal) {
            return new ParseResult(true, mealDate, sourceName, breakfast, lunch, dinner,
                    breakfastKcal, lunchKcal, dinnerKcal, totalKcal);
        }
    }

    public record ImportSummary(int inserted, int updated, int skipped, List<String> failures) {
    }
}
