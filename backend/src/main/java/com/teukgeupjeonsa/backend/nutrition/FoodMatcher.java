package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FoodMatcher {

    private static final Set<String> RICE_NAMES = Set.of("밥", "쌀밥", "백미밥", "잡곡밥", "흰밥", "현미밥", "멥쌀밥");
    private static final List<String> DESSERT_KEYWORDS = List.of("아이스크림", "빙수", "음료", "주스", "쥬스", "라떼", "푸딩", "젤리", "케이크", "쿠키", "초코", "디저트");
    private static final double SIMILARITY_THRESHOLD = 0.82;
    private static final int MAX_TOKEN_SEARCHES = 3;
    private static final int MAX_SIMILARITY_PREFIX_SEARCHES = 2;
    private static final double CONTAINS_THRESHOLD = 0.78;
    private static final double TOKEN_THRESHOLD = 0.78;

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final FoodNameNormalizer normalizer;
    private final FoodMatchOverrideProvider overrideProvider;
    private final ServingEstimator servingEstimator;

    @Transactional(readOnly = true)
    public FoodMatchResult match(String originalMenuName) {
        String original = Optional.ofNullable(originalMenuName).orElse("").trim();
        String normalized = normalizer.normalize(original);
        String searchName = normalizer.toSearchName(normalized);
        if (searchName.isBlank()) {
            return FoodMatchResult.noMatch(original, normalized);
        }

        Optional<ManualFoodOverride> override = overrideProvider.findOverride(searchName);
        if (override.isPresent()) {
            ManualFoodOverride foodOverride = override.get();
            return matched(original, normalized, foodOverride.getFood(), "OVERRIDE_EXACT",
                    Optional.ofNullable(foodOverride.getConfidence()).orElse(MatchConfidence.HIGH), 1.0, foodOverride.getDefaultServingGram());
        }

        Optional<FoodAlias> alias = foodAliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc(searchName);
        if (alias.isPresent() && isCompatible(searchName, alias.get().getFood())) {
            return matched(original, normalized, initializeFood(alias.get().getFood()), "ALIAS_EXACT", MatchConfidence.HIGH, 1.0, null);
        }

        Optional<Food> exactFood = foodRepository.findFirstBySearchNameOrderBySourceCountDesc(searchName);
        if (exactFood.isPresent() && isCompatible(searchName, exactFood.get())) {
            return matched(original, normalized, exactFood.get(), "FOOD_EXACT", MatchConfidence.HIGH, 1.0, null);
        }

        if (isRiceMenu(searchName)) {
            return findRiceContains(searchName)
                    .map(food -> matched(original, normalized, food, "RICE_FALLBACK", MatchConfidence.MEDIUM, 0.88, null))
                    .orElseGet(() -> FoodMatchResult.noMatch(original, normalized));
        }

        if (isShortRiskyQuery(searchName)) {
            return FoodMatchResult.noMatch(original, normalized);
        }

        if (isCompositeLikeMenu(searchName)) {
            return FoodMatchResult.noMatch(original, normalized);
        }

        Optional<FoodScore> contains = findContains(searchName);
        if (contains.isPresent()) {
            FoodScore score = contains.get();
            return matched(original, normalized, score.food(), "CONTAINS", MatchConfidence.MEDIUM, score.score(), null);
        }

        Optional<FoodScore> tokenMatch = findTokenMatch(searchName);
        if (tokenMatch.isPresent()) {
            FoodScore score = tokenMatch.get();
            return matched(original, normalized, score.food(), "TOKEN_CONTAINS", MatchConfidence.MEDIUM, score.score(), null);
        }

        Optional<FoodScore> similarity = findSimilarity(searchName);
        return similarity
                .map(score -> matched(original, normalized, score.food(), "SIMILARITY", MatchConfidence.LOW, score.score(), null))
                .orElseGet(() -> FoodMatchResult.noMatch(original, normalized));
    }

    private Optional<Food> findRiceContains(String searchName) {
        return safeFoods(foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase("밥", "밥", PageRequest.of(0, 50))).stream()
                .filter(food -> isCompatible(searchName, food))
                .filter(this::isPlausiblePlainRiceFood)
                .max(Comparator
                        .comparingInt((Food food) -> RICE_NAMES.contains(toComparableName(food)) ? 1 : 0)
                        .thenComparingInt(food -> Optional.ofNullable(food.getSourceCount()).orElse(0)));
    }

    private Optional<FoodScore> findContains(String searchName) {
        if (searchName.length() < 2) {
            return Optional.empty();
        }

        List<FoodScore> candidates = new java.util.ArrayList<>();
        safeFoods(foodRepository.searchContains(searchName, PageRequest.of(0, 50))).stream()
                .map(food -> new FoodScore(food, evidenceScore(searchName, toComparableName(food))))
                .forEach(candidates::add);
        safeAliases(foodAliasRepository.searchContains(searchName, PageRequest.of(0, 50))).stream()
                .map(alias -> new FoodScore(alias.getFood(), evidenceScore(searchName, normalizer.toSearchName(alias.getOriginalName()))))
                .forEach(candidates::add);

        return candidates.stream()
                .filter(score -> isFuzzyCandidateSafe(score.food()))
                .filter(score -> isCompatible(searchName, score.food()))
                .filter(score -> score.score() >= CONTAINS_THRESHOLD)
                .max(Comparator
                        .comparingDouble(FoodScore::score)
                        .thenComparing(score -> Optional.ofNullable(score.food().getSourceCount()).orElse(0)));
    }

    private Optional<FoodScore> findTokenMatch(String searchName) {
        return meaningfulTokens(searchName).stream()
                .filter(token -> token.length() >= 2 && !token.equals(searchName))
                .flatMap(token -> {
                    List<FoodScore> candidates = new java.util.ArrayList<>();
                    safeFoods(foodRepository.searchContains(token, PageRequest.of(0, 30))).stream()
                            .map(food -> new FoodScore(food, tokenScore(searchName, token, toComparableName(food))))
                            .forEach(candidates::add);
                    safeAliases(foodAliasRepository.searchContains(token, PageRequest.of(0, 30))).stream()
                            .map(alias -> new FoodScore(alias.getFood(), tokenScore(searchName, token, normalizer.toSearchName(alias.getOriginalName()))))
                            .forEach(candidates::add);
                    return candidates.stream()
                            .filter(score -> isFuzzyCandidateSafe(score.food()))
                            .filter(score -> isCompatible(searchName, score.food()))
                            .filter(score -> score.score() >= TOKEN_THRESHOLD);
                })
                .max(Comparator
                        .comparingDouble(FoodScore::score)
                        .thenComparing(score -> Optional.ofNullable(score.food().getSourceCount()).orElse(0)));
    }


    private List<Food> safeFoods(List<Food> foods) {
        return foods == null ? List.of() : foods;
    }

    private List<FoodAlias> safeAliases(List<FoodAlias> aliases) {
        return aliases == null ? List.of() : aliases;
    }

    private Optional<FoodScore> findSimilarity(String searchName) {
        if (searchName.length() <= 2 || isShortRiskyQuery(searchName)) {
            return Optional.empty();
        }

        List<FoodScore> candidates = new java.util.ArrayList<>();
        int searchedPrefixes = 0;
        for (int end = Math.min(searchName.length(), 5); end >= 2 && searchedPrefixes < MAX_SIMILARITY_PREFIX_SEARCHES; end--) {
            String token = searchName.substring(0, end);
            safeFoods(foodRepository.searchContains(token, PageRequest.of(0, 30))).stream()
                    .map(food -> new FoodScore(food, similarityScore(searchName, toComparableName(food))))
                    .forEach(candidates::add);
            safeAliases(foodAliasRepository.searchContains(token, PageRequest.of(0, 30))).stream()
                    .map(alias -> new FoodScore(alias.getFood(), similarityScore(searchName, normalizer.toSearchName(alias.getOriginalName()))))
                    .forEach(candidates::add);
            searchedPrefixes++;
        }

        return candidates.stream()
                .filter(score -> isFuzzyCandidateSafe(score.food()))
                .filter(score -> isCompatible(searchName, score.food()))
                .filter(score -> score.score() >= SIMILARITY_THRESHOLD)
                .max(Comparator
                        .comparingDouble(FoodScore::score)
                        .thenComparing(score -> Optional.ofNullable(score.food().getSourceCount()).orElse(0)));
    }

    private boolean isCompatible(String searchName, Food food) {
        if (food == null) {
            return false;
        }
        String foodName = toComparableName(food);
        String category = Optional.ofNullable(food.getCategory()).orElse("");
        if (isRiceMenu(searchName)) {
            return isPlausiblePlainRiceFood(food)
                    && !containsAny(foodName + category, DESSERT_KEYWORDS.toArray(String[]::new));
        }
        if (containsAny(searchName, "소스") && containsAny(category, "면", "라면")) {
            return false;
        }
        String combined = foodName + category;
        if (containsAny(searchName, "튀김", "치킨") && containsAny(combined, "소스", "장류", "양념")
                && !containsAny(combined, "튀김", "치킨", "닭")) return false;
        if (containsAny(searchName, "국", "탕", "찌개", "국밥")
                && !containsAny(combined, "국", "탕", "찌개", "국밥", "밥류")) return false;
        if (containsAny(searchName, "김치", "깍두기", "겉절이", "단무지")
                && !containsAny(searchName, "국", "탕", "찌개")
                && containsAny(combined, "국", "탕", "찌개")) return false;
        if (containsAny(searchName, "웰치", "콜라", "사이다", "음료", "주스")
                && !containsAny(combined, "음료", "주스", "탄산")) return false;
        if (containsAny(searchName, "숙회") && containsAny(combined, "과자", "빵", "비스킷", "간식")) return false;
        return true;
    }

    private boolean isRiceMenu(String searchName) {
        return RICE_NAMES.contains(searchName);
    }

    private boolean isPlausiblePlainRiceFood(Food food) {
        if (food == null || !RICE_NAMES.contains(toComparableName(food))) return false;
        Double kcal = food.getCalorie();
        Double carbohydrate = food.getCarbohydrate();
        Double protein = food.getProtein();
        Double fat = food.getFat();
        if (kcal == null || carbohydrate == null || protein == null || fat == null) return false;

        // foods 테이블은 100g 기준이다. 볶음밥·덮밥·오염 행을 일반 밥 fallback에서 제외한다.
        return kcal >= 100.0 && kcal <= 220.0
                && carbohydrate >= 20.0 && carbohydrate <= 50.0
                && protein >= 1.0 && protein <= 7.0
                && fat >= 0.0 && fat <= 3.0;
    }

    private boolean isCompositeLikeMenu(String searchName) {
        return containsAny(searchName, "볶음", "두루치기", "불고기", "조림", "구이", "스테이크", "튀김", "까스", "가스", "국", "탕", "찌개", "무침", "찜", "카레", "덮밥", "숙회", "떡볶이", "주먹밥", "겉절이", "짜글이")
                || (containsAny(searchName, "김치") && containsAny(searchName, "단무지"));
    }

    private boolean isShortRiskyQuery(String searchName) {
        return searchName.length() <= 2 && !isRiceMenu(searchName);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    private List<String> meaningfulTokens(String searchName) {
        List<String> commonFoodWords = List.of(
                "날치알", "파프리카", "오징어", "떡볶이", "주먹밥", "소머리국밥", "스테이크", "데리야끼",
                "닭다리", "단무지", "겉절이", "숙회", "어묵", "김치", "두부", "우유", "웰치", "카레", "찌개",
                "국밥", "조림", "볶음", "무침", "구이", "튀김", "국수", "라면", "샐러드", "소스", "계란", "국", "탕", "전", "밥"
        );
        List<String> tokens = new java.util.ArrayList<>();
        commonFoodWords.stream()
                .filter(searchName::contains)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(tokens::add);
        if (searchName.length() >= 4) {
            int size = Math.min(searchName.length(), 5);
            tokens.add(searchName.substring(0, size));
            tokens.add(searchName.substring(searchName.length() - size));
        }
        // Each token performs two DB searches. Keep the most meaningful tokens first
        // so a long military menu name cannot fan out into dozens of queries.
        return tokens.stream().filter(token -> token.length() >= 2).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .limit(MAX_TOKEN_SEARCHES).toList();
    }

    private double tokenScore(String searchName, String token, String evidenceName) {
        double coverage = token.length() / (double) Math.max(searchName.length(), 1);
        double evidence = evidenceScore(token, evidenceName);
        return Math.max(0.0, Math.min(0.90, 0.45 + coverage * 0.45 + evidence * 0.20));
    }

    private double evidenceScore(String source, String evidence) {
        if (source == null || evidence == null || source.isBlank() || evidence.isBlank()) return 0.0;
        if (source.equals(evidence)) return 1.0;
        if (source.contains(evidence) || evidence.contains(source)) {
            return Math.min(source.length(), evidence.length()) / (double) Math.max(source.length(), evidence.length());
        }
        return similarityScore(source, evidence);
    }

    private boolean isFuzzyCandidateSafe(Food food) {
        String quality = Optional.ofNullable(food.getQualityFlag()).orElse("").toLowerCase();
        return !containsAny(quality, "review", "outlier", "reject");
    }

    private double similarityScore(String source, String target) {
        if (source.isBlank() || target.isBlank()) {
            return 0;
        }
        int longest = longestCommonSubstring(source, target);
        double containment = longest / (double) Math.max(source.length(), target.length());
        double overlap = characterOverlap(source, target);
        return containment * 0.7 + overlap * 0.3;
    }

    private int longestCommonSubstring(String first, String second) {
        int[][] lengths = new int[first.length() + 1][second.length() + 1];
        int best = 0;
        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    lengths[i][j] = lengths[i - 1][j - 1] + 1;
                    best = Math.max(best, lengths[i][j]);
                }
            }
        }
        return best;
    }

    private double characterOverlap(String first, String second) {
        Set<Integer> firstChars = first.codePoints().boxed().collect(java.util.stream.Collectors.toSet());
        Set<Integer> secondChars = second.codePoints().boxed().collect(java.util.stream.Collectors.toSet());
        long common = firstChars.stream().filter(secondChars::contains).count();
        return common / (double) Math.max(firstChars.size(), secondChars.size());
    }

    private String toComparableName(Food food) {
        String searchName = food.getSearchName();
        if (searchName != null && !searchName.isBlank()) {
            return searchName.replaceAll("\\s+", "").toLowerCase();
        }
        return normalizer.toSearchName(food.getName());
    }

    private Food initializeFood(Food food) {
        food.getId();
        food.getName();
        food.getCategory();
        food.getServingUnit();
        food.getCalorie();
        food.getCarbohydrate();
        food.getProtein();
        food.getFat();
        return food;
    }

    private FoodMatchResult matched(String original, String normalized, Food food, String matchType, MatchConfidence confidence, double score, Double overrideServingGram) {
        Food initialized = initializeFood(food);
        double defaultServingGram = overrideServingGram != null ? overrideServingGram : servingEstimator.estimateGram(normalized, initialized.getCategory());
        return FoodMatchResult.builder()
                .originalMenuName(original)
                .normalizedMenuName(normalized)
                .matched(true)
                .matchedFoodId(initialized.getId())
                .matchedFoodName(initialized.getName())
                .displayCategory(initialized.getCategory())
                .matchType(matchType)
                .confidence(confidence)
                .score(score)
                .defaultServingGram(defaultServingGram)
                .matchedFood(initialized)
                .build();
    }

    private record FoodScore(Food food, double score) {
    }
}
