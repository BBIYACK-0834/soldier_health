package com.teukgeupjeonsa.backend.food.importer;

public record FoodImportResult(
        int foodCount,
        int aliasCount,
        int skippedFoodCount,
        int skippedAliasCount,
        int overrideCount,
        int servingDefaultCount
) {
}
