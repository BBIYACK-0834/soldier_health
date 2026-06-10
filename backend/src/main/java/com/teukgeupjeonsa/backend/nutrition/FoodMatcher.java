package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FoodMatcher {

    private static final Set<String> RICE_NAMES = Set.of("밥", "쌀밥", "백미밥", "잡곡밥", "흰밥", "현미밥");
    private static final List<String> DESSERT_KEYWORDS = List.of("아이스크림", "빙수", "음료", "주스", "쥬스", "라떼", "푸딩", "젤리", "케이크", "쿠키", "초코");
    private static final double SIMILARITY_THRESHOLD = 0.72;

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final FoodNameNormalizer normalizer;
    private final FoodMatchOverrideProvider overrideProvider;

    @Transactional(readOnly = true)
    public FoodMatchResult match(String originalMenuName) {
        String original = Optional.ofNullable(originalMenuName).orElse("").trim();
        String normalized = normalizer.normalize(original);
        String searchName = normalizer.toSearchName(normalized);
        if (searchName.isBlank()) {
            return FoodMatchResult.noMatch(original, normalized);
        }

        Optional<Food> override = overrideProvider.findOverride(searchName);
        if (override.isPresent()) {
            return matched(original, normalized, override.get(), "OVERRIDE_EXACT", MatchConfidence.HIGH, 1.0);
        }

        Optional<FoodAlias> alias = foodAliasRepository.findFirstBySearchNameOrderByFood_SourceCountDesc(searchName);
        if (alias.isPresent() && isCompatible(searchName, alias.get().getFood())) {
            return matched(original, normalized, initializeFood(alias.get().getFood()), "ALIAS_EXACT", MatchConfidence.HIGH, 1.0);
        }

        Optional<Food> exactFood = foodRepository.findFirstBySearchNameOrderBySourceCountDesc(searchName);
        if (exactFood.isPresent() && isCompatible(searchName, exactFood.get())) {
            return matched(original, normalized, exactFood.get(), "FOOD_EXACT", MatchConfidence.HIGH, 1.0);
        }

        if (isRiceMenu(searchName)) {
            return findRiceContains(searchName)
                    .map(food -> matched(original, normalized, food, "CONTAINS", MatchConfidence.MEDIUM, 0.88))
                    .orElseGet(() -> FoodMatchResult.noMatch(original, normalized));
        }

        Optional<Food> contains = findContains(searchName);
        if (contains.isPresent()) {
            return matched(original, normalized, contains.get(), "CONTAINS", MatchConfidence.MEDIUM, containsScore(searchName, contains.get()));
        }

        Optional<FoodScore> similarity = findSimilarity(searchName);
        return similarity
                .map(score -> matched(original, normalized, score.food(), "SIMILARITY", MatchConfidence.LOW, score.score()))
                .orElseGet(() -> FoodMatchResult.noMatch(original, normalized));
    }

    private Optional<Food> findRiceContains(String searchName) {
        return foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase("밥", "밥", PageRequest.of(0, 50)).stream()
                .filter(food -> isCompatible(searchName, food))
                .max(Comparator.comparingInt(food -> Optional.ofNullable(food.getSourceCount()).orElse(0)));
    }

    private Optional<Food> findContains(String searchName) {
        if (searchName.length() < 2) {
            return Optional.empty();
        }
        return foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(searchName, searchName, PageRequest.of(0, 50)).stream()
                .filter(food -> isCompatible(searchName, food))
                .max(Comparator
                        .comparingDouble((Food food) -> containsScore(searchName, food))
                        .thenComparing(food -> Optional.ofNullable(food.getSourceCount()).orElse(0)));
    }

    private Optional<FoodScore> findSimilarity(String searchName) {
        if (searchName.length() <= 2) {
            return Optional.empty();
        }

        Set<Food> candidates = new HashSet<>();
        for (int end = Math.min(searchName.length(), 5); end >= 2; end--) {
            String token = searchName.substring(0, end);
            candidates.addAll(foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(token, token, PageRequest.of(0, 30)));
        }

        return candidates.stream()
                .filter(food -> isCompatible(searchName, food))
                .map(food -> new FoodScore(food, similarityScore(searchName, toComparableName(food))))
                .filter(score -> score.score() >= SIMILARITY_THRESHOLD)
                .max(Comparator
                        .comparingDouble(FoodScore::score)
                        .thenComparing(score -> Optional.ofNullable(score.food().getSourceCount()).orElse(0)));
    }

    private boolean isCompatible(String searchName, Food food) {
        if (food == null) {
            return false;
        }
        if (isRiceMenu(searchName)) {
            String foodName = toComparableName(food);
            return containsAny(foodName, "밥", "쌀", "백미", "잡곡", "현미") && !containsAny(foodName, DESSERT_KEYWORDS.toArray(String[]::new));
        }
        return true;
    }

    private boolean isRiceMenu(String searchName) {
        return RICE_NAMES.contains(searchName);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double containsScore(String searchName, Food food) {
        String foodName = toComparableName(food);
        if (foodName.equals(searchName)) {
            return 1.0;
        }
        if (foodName.contains(searchName) || searchName.contains(foodName)) {
            return Math.min(searchName.length(), foodName.length()) / (double) Math.max(searchName.length(), foodName.length());
        }
        return 0.6;
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

    private FoodMatchResult matched(String original, String normalized, Food food, String matchType, MatchConfidence confidence, double score) {
        Food initialized = initializeFood(food);
        return FoodMatchResult.builder()
                .originalMenuName(original)
                .normalizedMenuName(normalized)
                .matched(true)
                .matchedFoodId(initialized.getId())
                .matchedFoodName(initialized.getName())
                .matchType(matchType)
                .confidence(confidence)
                .score(score)
                .matchedFood(initialized)
                .build();
    }

    private record FoodScore(Food food, double score) {
    }
}
