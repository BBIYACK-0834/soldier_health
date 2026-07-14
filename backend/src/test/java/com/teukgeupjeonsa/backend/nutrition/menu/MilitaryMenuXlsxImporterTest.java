package com.teukgeupjeonsa.backend.nutrition.menu;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MilitaryMenuXlsxImporterTest {

    @Test
    void importsBundledMilitaryMenuWorkbook() {
        MilitaryMenuProfileRepository profiles = mock(MilitaryMenuProfileRepository.class);
        MilitaryMenuUnitProfileRepository units = mock(MilitaryMenuUnitProfileRepository.class);
        when(profiles.saveAll(anyList())).thenAnswer(invocation -> invocation.<List<MilitaryMenuProfile>>getArgument(0));
        when(units.saveAll(anyList())).thenAnswer(invocation -> invocation.<List<MilitaryMenuUnitProfile>>getArgument(0));

        MilitaryMenuImportResult result = new MilitaryMenuXlsxImporter(profiles, units).importXlsx(
                Path.of("src/main/resources/food_data/military_menu_data.xlsx"));

        assertThat(result.menuCount()).isGreaterThan(10_000);
        assertThat(result.unitProfileCount()).isGreaterThan(30_000);
        assertThat(result.skippedMenuCount()).isGreaterThan(0);
        assertThat(result.skippedUnitProfileCount()).isGreaterThan(0);
    }
}
