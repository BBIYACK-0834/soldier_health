package com.teukgeupjeonsa.backend.collector.dto;

public record MealPersistResult(
        int inserted,
        int updated,
        int skipped
) {
    public static MealPersistResult empty() {
        return new MealPersistResult(0, 0, 0);
    }
}
