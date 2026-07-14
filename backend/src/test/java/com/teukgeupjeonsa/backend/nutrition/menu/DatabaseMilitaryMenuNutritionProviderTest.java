package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DatabaseMilitaryMenuNutritionProviderTest {

    @Test
    void exactDateValueTakesPriorityOverAggregatedProfiles() {
        MilitaryMenuProfileRepository profiles = mock(MilitaryMenuProfileRepository.class);
        MilitaryMenuUnitProfileRepository units = mock(MilitaryMenuUnitProfileRepository.class);
        MilitaryMenuDailyProfileRepository daily = mock(MilitaryMenuDailyProfileRepository.class);
        LocalDate date = LocalDate.of(2026, 7, 14);
        when(daily.findFirstByUnitCodeAndMealDateAndMealTypeAndSearchName(
                "7296", date, "dinner", "분모자마라찜닭"))
                .thenReturn(Optional.of(MilitaryMenuDailyProfile.builder()
                        .unitCode("7296").mealDate(date).mealType("dinner")
                        .searchName("분모자마라찜닭").canonicalName("분모자마라찜닭")
                        .calorieKcal(309.66).sampleCount(1).build()));

        MilitaryMenuNutritionMatch result = new DatabaseMilitaryMenuNutritionProvider(
                profiles, units, daily, new FoodNameNormalizer())
                .find("DS_TB_MNDT_DATEBYMLSVC_7296", date, "dinner", "분모자마라찜닭")
                .orElseThrow();

        assertThat(result.matchType()).isEqualTo("DAILY_UNIT_MENU");
        assertThat(result.calorieKcal()).isEqualTo(309.66);
        verifyNoInteractions(profiles, units);
    }
}
