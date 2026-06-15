package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.ManualFoodOverride;

import java.util.Optional;

public interface FoodMatchOverrideProvider {
    Optional<ManualFoodOverride> findOverride(String normalizedMenuName);
}
