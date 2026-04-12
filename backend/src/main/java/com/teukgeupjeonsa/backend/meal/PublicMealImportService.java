package com.teukgeupjeonsa.backend.meal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
import com.teukgeupjeonsa.backend.user.BranchType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicMealImportService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final MealDayRepository mealDayRepository;
    private final MilitaryUnitRepository militaryUnitRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${public-meal.api.base-url:}")
    private String baseUrl;

    @Value("${public-meal.api.service-key:}")
    private String serviceKey;

    @Value("${public-meal.api.rows:200}")
    private int rows;

    @Transactional
    public String importFromPublicApi(LocalDate startDate, LocalDate endDate, String unitKeyword) {
        if (baseUrl == null || baseUrl.isBlank() || serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException("공공 식단 API 설정이 없습니다. public-meal.api.base-url / service-key를 설정해주세요.");
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        int pageNo = 1;
        int importedCount = 0;

        while (true) {
            URI uri = buildUri(startDate, endDate, unitKeyword, pageNo);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            List<JsonNode> items = extractItems(response.getBody());
            if (items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                ImportedMeal importedMeal = toImportedMeal(item);
                if (importedMeal == null) {
                    continue;
                }

                MilitaryUnit unit = findOrCreateUnit(importedMeal.unitName());
                MealDay mealDay = mealDayRepository.findByUnitAndMealDate(unit, importedMeal.mealDate())
                        .orElseGet(() -> MealDay.builder().unit(unit).mealDate(importedMeal.mealDate()).build());

                mealDay.setBreakfastRaw(importedMeal.breakfast());
                mealDay.setLunchRaw(importedMeal.lunch());
                mealDay.setDinnerRaw(importedMeal.dinner());
                mealDay.setSourceName("public-meal-api");
                mealDayRepository.save(mealDay);
                importedCount++;
            }

            if (items.size() < rows) {
                break;
            }

            pageNo++;
        }

        return "공공 식단 API 수집 완료: " + importedCount + "건";
    }

    private URI buildUri(LocalDate startDate, LocalDate endDate, String unitKeyword, int pageNo) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("type", "json")
                .queryParam("numOfRows", rows)
                .queryParam("pageNo", pageNo)
                .queryParam("startDate", startDate.format(YMD))
                .queryParam("endDate", endDate.format(YMD));

        if (unitKeyword != null && !unitKeyword.isBlank()) {
            builder.queryParam("unitName", unitKeyword);
        }

        return builder.build(true).encode(StandardCharsets.UTF_8).toUri();
    }

    private List<JsonNode> extractItems(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.at("/response/body/items/item");
            if (items.isMissingNode() || items.isNull()) {
                items = root.at("/items/item");
            }
            if (items.isMissingNode() || items.isNull()) {
                items = root.at("/data");
            }

            if (items.isArray()) {
                List<JsonNode> result = new ArrayList<>();
                items.forEach(result::add);
                return result;
            }

            if (items.isObject()) {
                return List.of(items);
            }

            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private ImportedMeal toImportedMeal(JsonNode item) {
        String unitName = firstText(item,
                "unitName", "unitNm", "troopName", "baseName", "군부대명", "부대명");
        String dateRaw = firstText(item,
                "mealDate", "mealYmd", "mlsvYmd", "date", "급식일자");

        if (unitName == null || dateRaw == null) {
            return null;
        }

        LocalDate mealDate = parseDate(dateRaw);
        if (mealDate == null) {
            return null;
        }

        String breakfast = firstText(item,
                "breakfast", "breakfastMenu", "brkfstMenu", "조식", "조식메뉴");
        String lunch = firstText(item,
                "lunch", "lunchMenu", "lnchMenu", "중식", "중식메뉴");
        String dinner = firstText(item,
                "dinner", "dinnerMenu", "dnrMenu", "석식", "석식메뉴");

        return new ImportedMeal(unitName.trim(), mealDate, blankToNull(breakfast), blankToNull(lunch), blankToNull(dinner));
    }

    private LocalDate parseDate(String raw) {
        String normalized = raw.replaceAll("[^0-9-]", "").trim();

        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, YMD);
            }

            if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(normalized);
            }

            return null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private MilitaryUnit findOrCreateUnit(String unitName) {
        Optional<MilitaryUnit> existing = militaryUnitRepository.findByUnitNameIgnoreCase(unitName);
        if (existing.isPresent()) {
            return existing.get();
        }

        MilitaryUnit unit = MilitaryUnit.builder()
                .unitCode("AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .unitName(unitName)
                .branchType(BranchType.ETC)
                .regionName("미분류")
                .dataSourceKey("public-api")
                .build();

        return militaryUnitRepository.save(unit);
    }

    private String firstText(JsonNode node, String... candidates) {
        for (String candidate : candidates) {
            JsonNode value = node.get(candidate);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record ImportedMeal(String unitName, LocalDate mealDate, String breakfast, String lunch, String dinner) {
    }
}
