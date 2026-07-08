package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.nutrition.MealMenuItemParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealOpenApiCollectionServiceTest {

    private final MealOpenApiCollectionService service = new MealOpenApiCollectionService(
            null, null, null, null, null, null, null, null, new MealMenuItemParser()
    );

    @Test
    void mergeKcalIsIdempotentAndUsesMax() {
        assertThat(service.mergeKcal(954, 954)).isEqualTo(954);
        assertThat(service.mergeKcal(954, 900)).isEqualTo(954);
        assertThat(service.mergeKcal(null, 954)).isEqualTo(954);
    }

    @Test
    void mergeMealTextDeduplicatesByParsedItems() {
        String current = "청량음료\n코카콜라음료\n250mL\n캔";
        String incoming = "코카콜라음료\n250mL\n캔";

        assertThat(service.mergeMealText(current, incoming)).isEqualTo("코카콜라음료 250mL 캔");
    }
}
