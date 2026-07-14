package com.teukgeupjeonsa.backend.nutrition;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiceNutritionReferenceTest {

    @Test
    void estimatesPlainRiceMacrosFromOfficialCalories() {
        RiceNutritionReference.Macros macros = RiceNutritionReference.estimate(360);

        assertThat(macros.carbohydrateG()).isEqualTo(79.9);
        assertThat(macros.proteinG()).isEqualTo(6.5);
        assertThat(macros.fatG()).isEqualTo(0.7);
    }

    @Test
    void onlySupportsPlainRiceNames() {
        assertThat(RiceNutritionReference.supports("밥")).isTrue();
        assertThat(RiceNutritionReference.supports("백미밥")).isTrue();
        assertThat(RiceNutritionReference.supports("볶음밥")).isFalse();
        assertThat(RiceNutritionReference.supports("덮밥")).isFalse();
    }
}
