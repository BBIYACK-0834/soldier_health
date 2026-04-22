package com.teukgeupjeonsa.backend.unit;

public enum MealType {
    BREAKFAST,
    LUNCH,
    DINNER;

    public static MealType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("mealType은 breakfast/lunch/dinner 중 하나여야 합니다.");
        }

        return switch (value.trim().toLowerCase()) {
            case "breakfast", "아침" -> BREAKFAST;
            case "lunch", "점심" -> LUNCH;
            case "dinner", "저녁" -> DINNER;
            default -> throw new IllegalArgumentException("mealType은 breakfast/lunch/dinner 중 하나여야 합니다.");
        };
    }

    public String apiValue() {
        return name().toLowerCase();
    }
}
