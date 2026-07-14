package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import com.teukgeupjeonsa.backend.food.ServingDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CompositeFoodEstimator {

    private static final Map<String, List<String>> INGREDIENT_ALIASES = Map.ofEntries(
            Map.entry("돼지고기", List.of("돼지고기", "돈육")),
            Map.entry("돈육", List.of("돼지고기")),
            Map.entry("돈육채", List.of("돼지고기")),
            Map.entry("돈민찌", List.of("돼지고기 다짐육", "돼지고기")),
            Map.entry("제육", List.of("돼지고기")),
            Map.entry("계육", List.of("닭고기")),
            Map.entry("닭순살", List.of("닭고기")),
            Map.entry("계육채", List.of("닭고기")),
            Map.entry("닭고기", List.of("닭고기")),
            Map.entry("닭", List.of("닭고기")),
            Map.entry("닭다리", List.of("닭고기")),
            Map.entry("우육", List.of("소고기")),
            Map.entry("소고기", List.of("소고기")),
            Map.entry("쇠고기", List.of("소고기")),
            Map.entry("차돌", List.of("소고기")),
            Map.entry("쭈꾸미", List.of("주꾸미", "쭈꾸미")),
            Map.entry("주꾸미", List.of("주꾸미", "쭈꾸미")),
            Map.entry("삼겹살", List.of("삼겹살")),
            Map.entry("오징어", List.of("오징어")),
            Map.entry("연어", List.of("연어")),
            Map.entry("오징어채", List.of("오징어")),
            Map.entry("어묵", List.of("어묵")),
            Map.entry("비엔나", List.of("소시지")),
            Map.entry("햄", List.of("햄")),
            Map.entry("두부", List.of("두부")),
            Map.entry("김치", List.of("배추김치", "김치")),
            Map.entry("단무지", List.of("단무지")),
            Map.entry("파프리카", List.of("파프리카")),
            Map.entry("감자", List.of("감자")),
            Map.entry("버섯", List.of("버섯")),
            Map.entry("오이", List.of("오이")),
            Map.entry("양파", List.of("양파")),
            Map.entry("크리미양파드레싱", List.of("사우전아일랜드드레싱", "샐러드드레싱사우전드아일랜드", "시저드레싱")),
            Map.entry("드레싱", List.of("사우전아일랜드드레싱", "샐러드드레싱사우전드아일랜드", "시저드레싱")),
            Map.entry("부추", List.of("부추")),
            Map.entry("분모자", List.of("중국당면", "당면")),
            Map.entry("마라", List.of("마라소스", "고추기름")),
            Map.entry("무나물", List.of("무")),
            Map.entry("들깨", List.of("들깨")),
            Map.entry("떡볶이", List.of("떡볶이", "가래떡", "떡")),
            Map.entry("콩나물", List.of("콩나물")),
            Map.entry("숙주", List.of("숙주나물", "숙주")),
            Map.entry("야채", List.of("채소", "야채", "혼합채소")),
            Map.entry("채소", List.of("채소", "야채", "혼합채소")),
            Map.entry("카레", List.of("카레", "카레소스")),
            Map.entry("고추장", List.of("고추장")),
            Map.entry("데리야끼", List.of("데리야끼소스", "데리야끼"))
    );

    private static final List<String> COOKING_KEYWORDS = List.of(
            "두루치기", "불고기", "스테이크", "볶음", "조림", "구이", "튀김", "까스", "가스",
            "찌개", "국", "탕", "무침", "찜", "카레", "덮밥", "숙회", "떡볶이", "주먹밥", "겉절이", "짜글이"
    );

    private static final List<String> COMPOSED_FOOD_KEYWORDS = List.of(
            "김밥", "덮밥", "볶음밥", "비빔밥", "리조또", "라이스", "버거", "샌드위치"
    );

    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final FoodNameNormalizer normalizer;
    private final ServingDefaultRepository servingDefaultRepository;

    @Transactional(readOnly = true)
    public Optional<CompositeFoodEstimateResult> estimate(String originalMenuName) {
        String original = Optional.ofNullable(originalMenuName).orElse("").trim();
        String normalized = normalizer.toSearchName(original);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String method = detectCookingMethod(normalized);
        List<IngredientToken> ingredientTokens = extractIngredientTokens(normalized);
        if (ingredientTokens.isEmpty()) {
            return Optional.empty();
        }

        List<MatchedIngredient> matchedIngredients = new ArrayList<>();
        for (IngredientToken token : ingredientTokens) {
            findBestIngredientFood(token).ifPresent(matchedIngredients::add);
        }
        List<MatchedIngredient> distinctIngredients = dedupeIngredients(matchedIngredients);
        if (distinctIngredients.isEmpty()) {
            return Optional.empty();
        }

        MatchConfidence confidence = distinctIngredients.size() >= 2 ? MatchConfidence.MEDIUM : MatchConfidence.LOW;
        double servingGram = defaultServingGram(method);
        List<Double> ratios = ratiosFor(method, distinctIngredients.size());

        List<CompositeIngredientContribution> contributions = new ArrayList<>();
        double calorie = 0.0;
        double carb = 0.0;
        double protein = 0.0;
        double fat = 0.0;

        for (int i = 0; i < distinctIngredients.size(); i++) {
            MatchedIngredient ingredient = distinctIngredients.get(i);
            Food food = ingredient.food();
            double ratio = ratios.get(i);
            double gram = servingGram * ratio;
            double scale = gram / 100.0;

            Double ingredientCalorie = multiply(food.getCalorie(), scale);
            Double ingredientCarb = multiply(food.getCarbohydrate(), scale);
            Double ingredientProtein = multiply(food.getProtein(), scale);
            Double ingredientFat = multiply(food.getFat(), scale);

            calorie += nvl(ingredientCalorie);
            carb += nvl(ingredientCarb);
            protein += nvl(ingredientProtein);
            fat += nvl(ingredientFat);

            contributions.add(CompositeIngredientContribution.builder()
                    .foodId(food.getId())
                    .ingredientName(ingredient.sourceAlias())
                    .matchedFoodName(food.getName())
                    .ratio(round3(ratio))
                    .gram(round1(gram))
                    .calorieKcal(ingredientCalorie)
                    .carbohydrateG(ingredientCarb)
                    .proteinG(ingredientProtein)
                    .fatG(ingredientFat)
                    .build());
        }

        calorie += methodCalorieAdjustmentPerServing(method);
        String matchedDisplayName = contributions.stream()
                .map(CompositeIngredientContribution::getMatchedFoodName)
                .distinct()
                .collect(Collectors.joining(" + "));
        if (matchedDisplayName.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(CompositeFoodEstimateResult.builder()
                .originalMenuName(original)
                .normalizedMenuName(normalized)
                .cookingMethod(method)
                .servingGram(servingGram)
                .calorieKcal((int) Math.round(calorie))
                .carbohydrateG(round1(carb))
                .proteinG(round1(protein))
                .fatG(round1(fat))
                .confidence(confidence)
                .matchedDisplayName(matchedDisplayName + " 기반 추정")
                .ingredients(contributions)
                .build());
    }

    private String detectCookingMethod(String name) {
        if (name.contains("볶음") || name.contains("두루치기") || name.contains("불고기")) return "볶음";
        if (name.contains("조림")) return "조림";
        if (name.contains("구이") || name.contains("스테이크")) return "구이";
        if (name.contains("튀김") || name.contains("까스") || name.contains("가스")) return "튀김";
        if (name.contains("국") || name.contains("탕") || name.contains("찌개")) return "국탕찌개";
        if (name.contains("무침")) return "무침";
        if (name.contains("찜")) return "찜";
        if (name.contains("카레")) return "카레";
        if (name.contains("덮밥")) return "덮밥";
        if (name.contains("숙회")) return "숙회";
        if (name.contains("떡볶이")) return "떡볶이";
        if (name.contains("주먹밥")) return "주먹밥";
        if (name.contains("겉절이")) return "무침";
        if (name.contains("짜글이")) return "국탕찌개";
        return "일반";
    }

    private List<IngredientToken> extractIngredientTokens(String normalizedName) {
        String ingredientPart = removeCookingKeywords(normalizedName);
        List<String> aliases = INGREDIENT_ALIASES.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        boolean[] used = new boolean[ingredientPart.length()];
        List<IngredientToken> tokens = new ArrayList<>();

        for (String alias : aliases) {
            int start = ingredientPart.indexOf(alias);
            while (start >= 0) {
                int end = start + alias.length();
                if (!overlaps(used, start, end)) {
                    for (int i = start; i < end; i++) {
                        used[i] = true;
                    }
                    tokens.add(new IngredientToken(alias, INGREDIENT_ALIASES.get(alias), start));
                    break;
                }
                start = ingredientPart.indexOf(alias, start + 1);
            }
        }

        return tokens.stream()
                .sorted(Comparator.comparingInt(IngredientToken::position))
                .toList();
    }

    private String removeCookingKeywords(String normalizedName) {
        String result = normalizedName;
        for (String keyword : COOKING_KEYWORDS) {
            if ("카레".equals(keyword) || "스테이크".equals(keyword) || "떡볶이".equals(keyword)) {
                continue;
            }
            result = result.replace(keyword, "");
        }
        return result;
    }

    private boolean overlaps(boolean[] used, int start, int end) {
        for (int i = start; i < end; i++) {
            if (used[i]) return true;
        }
        return false;
    }

    private Optional<MatchedIngredient> findBestIngredientFood(IngredientToken token) {
        Map<Long, Candidate> candidates = new LinkedHashMap<>();
        for (String targetName : token.targetNames()) {
            String searchName = normalizer.toSearchName(targetName);
            if (searchName.isBlank()) continue;

            optionalFood(foodRepository.findFirstBySearchNameOrderBySourceCountDesc(searchName))
                    .ifPresent(food -> putBest(candidates, food, token, 120));

            for (FoodAlias alias : safeAliases(foodAliasRepository.findBySearchName(searchName))) {
                putBest(candidates, alias.getFood(), token, 115);
            }

            optionalFood(foodRepository.findFirstByNameOrderBySourceCountDesc(targetName))
                    .ifPresent(food -> putBest(candidates, food, token, 110));

            for (Food food : safeFoods(foodRepository.searchContains(searchName, PageRequest.of(0, 30)))) {
                putBest(candidates, food, token, 80 + containsScore(searchName, comparableName(food)));
            }

            for (FoodAlias alias : safeAliases(foodAliasRepository.searchContains(searchName, PageRequest.of(0, 30)))) {
                putBest(candidates, alias.getFood(), token, 75 + containsScore(searchName, aliasComparableName(alias)));
            }
        }

        return candidates.values().stream()
                .filter(candidate -> hasAnyNutrition(candidate.food()))
                .max(Comparator
                        .comparingInt(Candidate::score)
                        .thenComparing(candidate -> Optional.ofNullable(candidate.food().getSourceCount()).orElse(0)))
                .map(candidate -> new MatchedIngredient(token.aliasName(), candidate.food()));
    }



    private Optional<Food> optionalFood(Optional<Food> food) {
        return food == null ? Optional.empty() : food;
    }

    private List<Food> safeFoods(List<Food> foods) {
        return foods == null ? List.of() : foods;
    }

    private List<FoodAlias> safeAliases(List<FoodAlias> aliases) {
        return aliases == null ? List.of() : aliases;
    }

    private void putBest(Map<Long, Candidate> candidates, Food food, IngredientToken token, int score) {
        if (food == null || food.getId() == null) return;
        int adjustedScore = score - composedFoodPenalty(food) + ingredientSuitabilityScore(token, food);
        Candidate current = candidates.get(food.getId());
        Candidate next = new Candidate(token.aliasName(), food, adjustedScore);
        if (current == null || next.score() > current.score()) {
            candidates.put(food.getId(), next);
        }
    }

    private int containsScore(String query, String target) {
        if (target == null || target.isBlank()) return 0;
        if (target.equals(query)) return 25;
        if (target.contains(query)) return Math.max(5, 20 - (target.length() - query.length()));
        return 0;
    }

    private int composedFoodPenalty(Food food) {
        String name = comparableName(food);
        String category = Optional.ofNullable(food.getCategory()).orElse("");
        int penalty = COMPOSED_FOOD_KEYWORDS.stream().anyMatch(name::contains) ? 45 : 0;
        if (containsAny(category, "육류", "어패", "채소", "김치", "장아찌", "두부", "콩", "밥류", "양념")) {
            penalty -= 10;
        }
        return penalty;
    }

    private int ingredientSuitabilityScore(IngredientToken token, Food food) {
        String category = Optional.ofNullable(food.getCategory()).orElse("");
        String name = comparableName(food);
        String target = String.join("", token.targetNames());
        int score = nutritionCompletenessScore(food);

        if (containsAny(target, "감자") && containsAny(category, "감자 및 전분류", "채소류")) score += 35;
        if (containsAny(target, "양파", "오이", "부추", "버섯", "파프리카", "채소", "야채")
                && containsAny(category, "채소류", "버섯류")) score += 35;
        if (containsAny(target, "연어", "오징어", "주꾸미") && containsAny(category, "어류", "어패류", "수산물")) score += 35;
        if (containsAny(target, "소고기", "돼지고기", "닭고기") && containsAny(category, "육류", "식육")) score += 30;
        if (containsAny(target, "드레싱", "소스") && containsAny(category, "조미식품", "조미료", "소스", "장류")) score += 35;

        if (containsAny(category, "과자류", "빵류", "간식", "음료류")
                && !containsAny(target, "드레싱", "소스")) score -= 55;
        if (containsAny(name, "칩", "깡", "스낵", "과자", "주스", "즙")
                && !containsAny(target, "칩", "깡", "스낵", "주스", "즙")) score -= 45;
        return score;
    }

    private int nutritionCompletenessScore(Food food) {
        int populated = 0;
        if (food.getCalorie() != null) populated++;
        if (food.getCarbohydrate() != null) populated++;
        if (food.getProtein() != null) populated++;
        if (food.getFat() != null) populated++;
        if (food.getCalorie() == null) return -60 + populated * 4;
        return populated * 5;
    }

    private boolean hasAnyNutrition(Food food) {
        return food.getCalorie() != null || food.getCarbohydrate() != null || food.getProtein() != null || food.getFat() != null;
    }

    private List<MatchedIngredient> dedupeIngredients(List<MatchedIngredient> ingredients) {
        Map<Long, MatchedIngredient> byFood = new LinkedHashMap<>();
        for (MatchedIngredient ingredient : ingredients) {
            Food food = ingredient.food();
            if (food.getId() != null) {
                byFood.putIfAbsent(food.getId(), ingredient);
            }
        }
        return new ArrayList<>(byFood.values());
    }

    private double defaultServingGram(String method) {
        String category = switch (method) {
            case "볶음" -> "볶음류";
            case "조림" -> "조림류";
            case "구이" -> "구이류";
            case "튀김" -> "튀김류";
            case "국탕찌개" -> "국/탕/찌개류";
            case "무침" -> "무침류";
            case "찜" -> "찜류";
            case "카레" -> "카레류";
            case "덮밥" -> "덮밥류";
            default -> null;
        };
        if (category != null) {
            Optional<Double> fromDb = servingDefaultRepository.findFirstByCategory(category).map(defaultValue -> defaultValue.getServingGram());
            if (fromDb.isPresent()) {
                return fromDb.get();
            }
        }
        return switch (method) {
            case "볶음" -> 160.0;
            case "조림" -> 150.0;
            case "구이" -> 140.0;
            case "튀김" -> 130.0;
            case "국탕찌개" -> 300.0;
            case "무침" -> 80.0;
            case "찜" -> 180.0;
            case "카레" -> 180.0;
            case "덮밥" -> 350.0;
            default -> 120.0;
        };
    }

    private List<Double> ratiosFor(String method, int ingredientCount) {
        if (ingredientCount <= 0) return List.of();
        if ("국탕찌개".equals(method)) {
            if (ingredientCount == 1) return List.of(0.30);
            if (ingredientCount == 2) return List.of(0.20, 0.15);
            return distribute(0.35, ingredientCount);
        }
        if ("덮밥".equals(method)) {
            return distribute(0.75, ingredientCount);
        }
        if ("볶음".equals(method) || "조림".equals(method) || "구이".equals(method) || "찜".equals(method) || "카레".equals(method)) {
            if (ingredientCount == 1) return List.of(0.75);
            if (ingredientCount == 2) return List.of(0.45, 0.35);
            if (ingredientCount == 3) return List.of(0.40, 0.25, 0.20);
            return distribute(0.80, ingredientCount);
        }
        if ("튀김".equals(method)) {
            if (ingredientCount == 1) return List.of(0.70);
            return distribute(0.75, ingredientCount);
        }
        if ("무침".equals(method)) {
            if (ingredientCount == 1) return List.of(0.85);
            return distribute(0.85, ingredientCount);
        }
        return distribute(0.80, ingredientCount);
    }

    private List<Double> distribute(double totalRatio, int count) {
        if (count <= 0) return List.of();
        double each = totalRatio / count;
        return IntStream.range(0, count).mapToObj(i -> each).toList();
    }

    private double methodCalorieAdjustmentPerServing(String method) {
        return switch (method) {
            case "볶음" -> 45.0;
            case "조림" -> 35.0;
            case "튀김" -> 90.0;
            case "무침" -> 20.0;
            case "국탕찌개" -> 15.0;
            default -> 0.0;
        };
    }

    private Double multiply(Double value, double scale) {
        return value == null ? null : round1(value * scale);
    }

    private double nvl(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String comparableName(Food food) {
        String searchName = food.getSearchName();
        if (searchName != null && !searchName.isBlank()) {
            return searchName.replaceAll("\\s+", "").toLowerCase();
        }
        return normalizer.toSearchName(food.getName());
    }

    private String aliasComparableName(FoodAlias alias) {
        String searchName = alias.getSearchName();
        if (searchName != null && !searchName.isBlank()) {
            return searchName.replaceAll("\\s+", "").toLowerCase();
        }
        return normalizer.toSearchName(alias.getAliasName());
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value != null && value.contains(keyword)) return true;
        }
        return false;
    }

    private record IngredientToken(String aliasName, List<String> targetNames, int position) {}
    private record Candidate(String sourceAlias, Food food, int score) {}
    private record MatchedIngredient(String sourceAlias, Food food) {}
}
