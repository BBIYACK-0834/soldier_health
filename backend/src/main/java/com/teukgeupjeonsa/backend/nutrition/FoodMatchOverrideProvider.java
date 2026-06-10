package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;

import java.util.Optional;

public interface FoodMatchOverrideProvider {
    Optional<Food> findOverride(String normalizedMenuName);
}
