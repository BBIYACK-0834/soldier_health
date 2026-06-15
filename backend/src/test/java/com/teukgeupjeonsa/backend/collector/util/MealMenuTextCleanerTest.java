package com.teukgeupjeonsa.backend.collector.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealMenuTextCleanerTest {

    private final MealMenuTextCleaner cleaner = new MealMenuTextCleaner();

    @Test
    void removeOnlyNumericAllergyCodes() {
        assertThat(cleaner.cleanMealText("닭장각삼계탕(02)(05)(06)(15)(16)"))
                .isEqualTo("닭장각삼계탕");

        assertThat(cleaner.cleanMealText("해물완자전(01)(02)(05)(06)(08)(09)(12)(16)(17)(18)"))
                .isEqualTo("해물완자전");

        assertThat(cleaner.cleanMealText("오이고추된장무침(05)(06), 배추김치(09)"))
                .isEqualTo("오이고추된장무침, 배추김치");
    }

    @Test
    void keepNonNumericParentheses() {
        assertThat(cleaner.cleanMealText("우유(백색우유( 연간))(02)"))
                .isEqualTo("우유(백색우유( 연간))");

        assertThat(cleaner.cleanMealText("대만식샌드위치(완)"))
                .isEqualTo("대만식샌드위치(완)");

        assertThat(cleaner.cleanMealText("백김치(수의)"))
                .isEqualTo("백김치(수의)");
    }
}
