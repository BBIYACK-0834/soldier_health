package com.teukgeupjeonsa.backend.nutrition.matching;

import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import com.teukgeupjeonsa.backend.nutrition.MealNutritionService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MealMenuOccurrenceService {
    private final MealMenuRepository mealMenuRepository;
    private final MealMenuOccurrenceRepository occurrenceRepository;
    private final MealNutritionService mealNutritionService;
    private final FoodNameNormalizer normalizer;

    @Transactional
    public RebuildResult rebuildOccurrences() {
        Map<String, Accumulator> byNormalizedName = new LinkedHashMap<>();
        for (MealMenu meal : mealMenuRepository.findAll()) {
            collect(byNormalizedName, meal, meal.getBreakfast());
            collect(byNormalizedName, meal, meal.getLunch());
            collect(byNormalizedName, meal, meal.getDinner());
        }
        occurrenceRepository.deleteAllInBatch();
        List<MealMenuOccurrence> saved = occurrenceRepository.saveAll(byNormalizedName.values().stream()
                .map(Accumulator::toEntity)
                .toList());
        return RebuildResult.builder().processedMenus(byNormalizedName.values().stream().mapToInt(Accumulator::count).sum()).savedOccurrences(saved.size()).build();
    }

    private void collect(Map<String, Accumulator> byNormalizedName, MealMenu meal, String rawMenu) {
        for (String item : mealNutritionService.parseMealItems(rawMenu)) {
            String normalized = normalizer.normalize(item);
            String searchName = normalizer.toSearchName(normalized);
            if (searchName.isBlank()) continue;
            byNormalizedName.computeIfAbsent(searchName, key -> new Accumulator(item, searchName))
                    .add(item, meal.getMealDate(), meal.getServiceCode());
        }
    }

    @Getter @Builder
    public static class RebuildResult {
        private int processedMenus;
        private int savedOccurrences;
    }

    private static class Accumulator {
        private final String normalized;
        private String sampleRaw;
        private String sampleServiceCode;
        private int count;
        private LocalDate lastSeenDate;

        Accumulator(String raw, String normalized) {
            this.sampleRaw = raw;
            this.normalized = normalized;
        }
        void add(String raw, LocalDate date, String serviceCode) {
            count++;
            if (lastSeenDate == null || (date != null && date.isAfter(lastSeenDate))) {
                lastSeenDate = date;
                sampleRaw = raw;
                sampleServiceCode = serviceCode;
            }
        }
        int count() { return count; }
        MealMenuOccurrence toEntity() {
            return MealMenuOccurrence.builder()
                    .rawMenuName(sampleRaw)
                    .normalizedMenuName(normalized)
                    .occurrenceCount(count)
                    .lastSeenDate(lastSeenDate)
                    .sampleServiceCode(sampleServiceCode)
                    .build();
        }
    }
}
