package com.teukgeupjeonsa.backend.nutrition;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodNameNormalizerTest {

    private final FoodNameNormalizer normalizer = new FoodNameNormalizer();

    @Test
    void removesAllergyNumbersAndParentheses() {
        assertThat(normalizer.normalize("꽁치김치찌개(05)(06)(09)(16)")).isEqualTo("꽁치김치찌개");
    }

    @Test
    void removesOperationWordsAndConcatenatesAmpersandItems() {
        assertThat(normalizer.normalize("팥빙수 부대계약(02)(05)")).isEqualTo("팥빙수");
        assertThat(normalizer.normalize("깍두기(수의계약)")).isEqualTo("깍두기");
        assertThat(normalizer.normalize("백김치(수의)")).isEqualTo("백김치");
        assertThat(normalizer.normalize("햄전&케찹(01)(05)(06)(10)(12)(13)")).isEqualTo("햄전케찹");
    }

    @Test
    void preservesUsefulTextInsideParenthesesAndRemovesPackageSize() {
        assertThat(normalizer.normalize("우유(백색우유(200ML 연간))")).isEqualTo("우유 백색우유");
        assertThat(normalizer.normalize("가공우유(바나나맛)(02)")).isEqualTo("가공우유 바나나맛");
    }
}
