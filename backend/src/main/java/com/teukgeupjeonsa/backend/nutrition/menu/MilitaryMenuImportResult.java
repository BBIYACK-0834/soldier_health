package com.teukgeupjeonsa.backend.nutrition.menu;

public record MilitaryMenuImportResult(
        int menuCount, int unitProfileCount, int dailyProfileCount,
        int skippedMenuCount, int skippedUnitProfileCount) { }
