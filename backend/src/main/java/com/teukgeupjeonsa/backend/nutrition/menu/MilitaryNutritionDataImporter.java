package com.teukgeupjeonsa.backend.nutrition.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class MilitaryNutritionDataImporter {
    private final MilitaryMenuXlsxImporter profileImporter;
    private final MilitaryMenuDailyCsvImporter dailyImporter;

    @Transactional
    public MilitaryMenuImportResult importAll(Path profileXlsx, Path dailyCsvGzip) {
        MilitaryMenuImportResult profiles = profileImporter.importXlsx(profileXlsx);
        int dailyCount = dailyImporter.importGzipCsv(dailyCsvGzip);
        return new MilitaryMenuImportResult(profiles.menuCount(), profiles.unitProfileCount(), dailyCount,
                profiles.skippedMenuCount(), profiles.skippedUnitProfileCount());
    }
}
