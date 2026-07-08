package com.teukgeupjeonsa.backend.nutrition;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealMenuItemParserTest {

    private final MealMenuItemParser parser = new MealMenuItemParser();

    @Test
    void mergesSoftDrinkVolumeAndPackage() {
        String raw = "청량음료\n코카콜라음료\n\n250mL\n캔";

        assertThat(parser.parse(raw)).containsExactly("코카콜라음료 250mL 캔");
    }

    @Test
    void mergesMilkParenthesisAcrossLines() {
        String raw = "우유(백색우유(200ML\n연간))";

        assertThat(parser.parse(raw)).containsExactly("백색우유 200ML");
    }

    @Test
    void parsesFullBreakfastWithoutStandaloneUnits() {
        String raw = "언양식불고기버거\n맛감자튀김\n우유(백색우유(200ML\n연간))\n청량음료\n코카콜라음료\n\n250mL\n캔";

        assertThat(parser.parse(raw)).containsExactly(
                "언양식불고기버거",
                "맛감자튀김",
                "백색우유 200ML",
                "코카콜라음료 250mL 캔"
        );
        assertThat(parser.parse(raw)).doesNotContain("250mL", "캔", "연간", "청량음료");
    }

    @Test
    void preservesUsefulParenthesisTextAndRemovesAllergyCode() {
        assertThat(parser.parse("가공우유(바나나맛)(02)")).containsExactly("가공우유 바나나맛");
    }
}
