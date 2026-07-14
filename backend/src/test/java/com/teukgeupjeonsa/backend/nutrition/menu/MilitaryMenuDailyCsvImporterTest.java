package com.teukgeupjeonsa.backend.nutrition.menu;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MilitaryMenuDailyCsvImporterTest {

    @Test
    void validatesAndStreamsBundledDailyProfiles() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        int imported = new MilitaryMenuDailyCsvImporter(jdbcTemplate).importGzipCsv(
                Path.of("src/main/resources/food_data/military_menu_daily_profiles.csv.gz"));

        assertThat(imported).isEqualTo(373_489);
        verify(jdbcTemplate).update("delete from military_menu_daily_profiles");
        verify(jdbcTemplate, atLeastOnce()).batchUpdate(anyString(), anyList());
    }
}
