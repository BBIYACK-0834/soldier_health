package com.teukgeupjeonsa.backend.nutrition.menu;

import java.util.Optional;
import java.time.LocalDate;

public class EmptyMilitaryMenuNutritionProvider implements MilitaryMenuNutritionProvider {
    @Override
    public Optional<MilitaryMenuNutritionMatch> find(String serviceCode, LocalDate mealDate, String mealType, String rawMenuName) {
        return Optional.empty();
    }
}
