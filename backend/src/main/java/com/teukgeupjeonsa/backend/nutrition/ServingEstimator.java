package com.teukgeupjeonsa.backend.nutrition;

import org.springframework.stereotype.Component;

@Component
public class ServingEstimator {

    public double estimateGram(String normalizedName, String category) {
        String value = ((normalizedName == null ? "" : normalizedName) + " " + (category == null ? "" : category)).replaceAll("\\s+", "");
        if (containsAny(value, "쌀밥", "백미밥", "잡곡밥", "현미밥") || "밥".equals(value)) return 210.0;
        if (containsAny(value, "국", "찌개", "탕")) return 200.0;
        if (containsAny(value, "고기", "불고기", "닭", "돼지", "소고기", "생선", "조림", "볶음")) return 120.0;
        if (containsAny(value, "전", "튀김", "까스", "가스")) return 90.0;
        if (containsAny(value, "나물", "무침")) return 50.0;
        if (containsAny(value, "김치", "겉절이", "깍두기")) return 40.0;
        if (containsAny(value, "빙수", "아이스크림", "과일", "푸딩", "젤리", "케이크", "쿠키", "디저트")) return 120.0;
        return 100.0;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
