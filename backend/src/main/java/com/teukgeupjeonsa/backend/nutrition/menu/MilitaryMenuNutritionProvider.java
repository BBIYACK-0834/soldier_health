package com.teukgeupjeonsa.backend.nutrition.menu;

import java.util.Optional;
import java.time.LocalDate;

public interface MilitaryMenuNutritionProvider {
    Optional<MilitaryMenuNutritionMatch> find(String serviceCode, LocalDate mealDate, String mealType, String rawMenuName);

    default Optional<MilitaryMenuNutritionMatch> find(String serviceCode, String rawMenuName) {
        return find(serviceCode, null, null, rawMenuName);
    }
}
