package com.teukgeupjeonsa.backend.food.importer;

public record FoodImportResult(int foodCount, int aliasCount, int skippedFoodCount, int skippedAliasCount,
                               int manualOverrideCount, int servingDefaultCount) {
    public FoodImportResult(int foodCount, int aliasCount, int skippedFoodCount, int skippedAliasCount) {
        this(foodCount, aliasCount, skippedFoodCount, skippedAliasCount, 0, 0);
    }
}
