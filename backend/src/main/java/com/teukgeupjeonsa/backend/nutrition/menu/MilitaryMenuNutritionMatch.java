package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;

public record MilitaryMenuNutritionMatch(
        String canonicalName,
        String category,
        double calorieKcal,
        MatchConfidence confidence,
        String matchType,
        int sampleCount,
        String unitCode
) { }
