package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class MealNutritionService {

    private static final long ITEM_CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private static final List<String> SAMPLE_MENUS = List.of(
            "밥",
            "꽁치김치찌개(05)(06)(09)(16)",
            "닭순살카레조림(05)(06)(15)",
            "햄전&케찹(01)(05)(06)(10)(12)(13)",
            "얼갈이된장무침(05)(06)(16)",
            "배추겉절이",
            "팥빙수 부대계약(02)(05)"
    );

    private final FoodNameNormalizer foodNameNormalizer;
    private final FoodMatcher foodMatcher;
    private final ServingEstimator servingEstimator;
    private final NutritionCalculator nutritionCalculator;
    private final CompositeFoodEstimator compositeFoodEstimator;
    private final MealMenuItemParser mealMenuItemParser;
    private final ConcurrentMap<String, CachedItem> analyzedItemCache = new ConcurrentHashMap<>();

    public NutritionDtos.MealNutritionResponse analyzeMeal(String mealType, String rawMenu, Integer officialCalorieKcal) {
        return analyzeMeal(mealType, mealMenuItemParser.parse(rawMenu), officialCalorieKcal);
    }

    public NutritionDtos.MealNutritionResponse analyzeMeal(String mealType, List<String> menuNames, Integer officialCalorieKcal) {
        List<NutritionDtos.MealNutritionItemResponse> items = new ArrayList<>();
        for (String menuName : menuNames) {
            if (menuName == null || menuName.isBlank()) {
                continue;
            }
            items.add(copyForMealType(analyzeItemCached(menuName), mealType));
        }
        return buildResponse(mealType, officialCalorieKcal, items);
    }

    private NutritionDtos.MealNutritionItemResponse analyzeItemCached(String menuName) {
        String key = foodNameNormalizer.toSearchName(menuName);
        long now = System.currentTimeMillis();
        CachedItem cached = analyzedItemCache.get(key);
        if (cached != null && now - cached.createdAtMillis() < ITEM_CACHE_TTL_MILLIS) {
            return cached.item();
        }
        NutritionDtos.MealNutritionItemResponse analyzed = analyzeItemTemplate(menuName);
        analyzedItemCache.put(key, new CachedItem(analyzed, now));
        return analyzed;
    }

    public void clearAnalysisCache() {
        analyzedItemCache.clear();
    }

    public NutritionDtos.MealNutritionResponse buildSampleMealNutritionResponse() {
        return analyzeMeal("sample", SAMPLE_MENUS, 900);
    }

    private NutritionDtos.MealNutritionItemResponse analyzeItemTemplate(String menuName) {
        String mealType = "";
        String normalizedName = foodNameNormalizer.normalize(menuName);
        FoodMatchResult match = foodMatcher.match(menuName);
        if (match.isMatched()) {
            return buildMatchedItem(mealType, menuName, normalizedName, match);
        }

        Optional<CompositeFoodEstimateResult> composite = compositeFoodEstimator.estimate(menuName);
        if (composite.isPresent()) {
            return buildCompositeItem(mealType, menuName, normalizedName, composite.get());
        }

        return buildNoMatchItem(mealType, menuName, normalizedName, match);
    }

    private NutritionDtos.MealNutritionItemResponse buildMatchedItem(String mealType, String menuName, String normalizedName, FoodMatchResult match) {
        Food food = match.getMatchedFood();
        double servingGram = match.getDefaultServingGram() != null ? match.getDefaultServingGram() : servingEstimator.estimateGram(normalizedName, food == null ? null : food.getCategory());
        NutritionCalculator.CalculatedNutrition nutrition = nutritionCalculator.calculate(match, servingGram);
        Integer calorieKcal = nutrition.calorieKcal() == null ? null : (int) Math.round(nutrition.calorieKcal());

        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(menuName)
                .normalizedName(normalizedName)
                .matched(true)
                .matchedFoodName(match.getMatchedFoodName())
                .displayCategory(match.getDisplayCategory())
                .matchType(match.getMatchType())
                .confidence(match.getConfidence())
                .servingGram(servingGram)
                .calorieKcal(calorieKcal)
                .carbohydrateG(nutrition.carbohydrateG())
                .proteinG(nutrition.proteinG())
                .fatG(nutrition.fatG())
                .foodId(match.getMatchedFoodId())
                .foodName(menuName)
                .mealType(mealType)
                .category(food == null ? null : food.getCategory())
                .servingUnit(food == null ? null : food.getServingUnit())
                .calories(calorieKcal)
                .carbG(nutrition.carbohydrateG())
                .addedByUser(false)
                .matchStatus("MATCHED")
                .build();
    }

    private NutritionDtos.MealNutritionItemResponse buildCompositeItem(String mealType, String menuName, String normalizedName, CompositeFoodEstimateResult estimate) {
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(menuName)
                .normalizedName(normalizedName)
                .matched(true)
                .matchedFoodName(estimate.getMatchedDisplayName())
                .displayCategory("복합 음식 추정")
                .matchType("COMPOSITE_ESTIMATE")
                .confidence(estimate.getConfidence())
                .servingGram(estimate.getServingGram())
                .calorieKcal(estimate.getCalorieKcal())
                .carbohydrateG(estimate.getCarbohydrateG())
                .proteinG(estimate.getProteinG())
                .fatG(estimate.getFatG())
                .foodId(null)
                .foodName(menuName)
                .mealType(mealType)
                .category("복합 음식 추정")
                .servingUnit("추정 " + Math.round(estimate.getServingGram()) + "g")
                .calories(estimate.getCalorieKcal())
                .carbG(estimate.getCarbohydrateG())
                .addedByUser(false)
                .matchStatus("COMPOSITE_ESTIMATE")
                .build();
    }

    private NutritionDtos.MealNutritionItemResponse buildNoMatchItem(String mealType, String menuName, String normalizedName, FoodMatchResult match) {
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(menuName)
                .normalizedName(normalizedName)
                .matched(false)
                .matchedFoodName(match.getMatchedFoodName())
                .displayCategory(match.getDisplayCategory())
                .matchType(match.getMatchType())
                .confidence(match.getConfidence())
                .servingGram(null)
                .calorieKcal(null)
                .carbohydrateG(null)
                .proteinG(null)
                .fatG(null)
                .foodId(null)
                .foodName(menuName)
                .mealType(mealType)
                .category(null)
                .servingUnit(null)
                .calories(null)
                .carbG(null)
                .addedByUser(false)
                .matchStatus("UNMATCHED")
                .build();
    }

    private NutritionDtos.MealNutritionItemResponse copyForMealType(NutritionDtos.MealNutritionItemResponse item, String mealType) {
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(item.getMenuName())
                .normalizedName(item.getNormalizedName())
                .matched(item.getMatched())
                .matchedFoodName(item.getMatchedFoodName())
                .displayCategory(item.getDisplayCategory())
                .matchType(item.getMatchType())
                .confidence(item.getConfidence())
                .servingGram(item.getServingGram())
                .calorieKcal(item.getCalorieKcal())
                .carbohydrateG(item.getCarbohydrateG())
                .proteinG(item.getProteinG())
                .fatG(item.getFatG())
                .foodId(item.getFoodId())
                .foodName(item.getFoodName())
                .mealType(mealType)
                .category(item.getCategory())
                .servingUnit(item.getServingUnit())
                .calories(item.getCalories())
                .carbG(item.getCarbG())
                .calorieSharePct(item.getCalorieSharePct())
                .addedByUser(item.getAddedByUser())
                .matchStatus(item.getMatchStatus())
                .build();
    }

    public NutritionDtos.MealNutritionResponse buildResponse(String mealType, Integer officialCalorieKcal, List<NutritionDtos.MealNutritionItemResponse> items) {
        return buildResponse(mealType, officialCalorieKcal, items, 0.0);
    }

    public NutritionDtos.MealNutritionResponse buildResponse(String mealType, Integer officialCalorieKcal,
                                                               List<NutritionDtos.MealNutritionItemResponse> items,
                                                               double consumptionMultiplier) {
        int matchedItemCount = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getMatched())).count();
        int totalItemCount = items.size();
        int estimatedItemCalories = items.stream()
                .map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int providedRawEstimatedCalories = items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getAddedByUser()))
                .map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        double matchedRatioRaw = totalItemCount == 0 ? 0.0 : matchedItemCount / (double) totalItemCount;
        double calibrationScale = calibrationScale(officialCalorieKcal, providedRawEstimatedCalories, matchedRatioRaw);
        List<NutritionDtos.MealNutritionItemResponse> calibrated = items.stream()
                .map(item -> Boolean.TRUE.equals(item.getAddedByUser()) ? item : scaleItem(item, calibrationScale))
                .toList();
        int addedCalories = calibrated.stream().filter(item -> Boolean.TRUE.equals(item.getAddedByUser()))
                .map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        int providedEstimatedCalories = calibrated.stream().filter(item -> !Boolean.TRUE.equals(item.getAddedByUser()))
                .map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        int providedCalories = officialCalorieKcal != null ? officialCalorieKcal : providedEstimatedCalories;
        int displayCalories = providedCalories + addedCalories;
        double carbohydrate = calibrated.stream().mapToDouble(item -> Optional.ofNullable(item.getCarbohydrateG()).orElse(0.0)).sum();
        double protein = calibrated.stream().mapToDouble(item -> Optional.ofNullable(item.getProteinG()).orElse(0.0)).sum();
        double fat = calibrated.stream().mapToDouble(item -> Optional.ofNullable(item.getFatG()).orElse(0.0)).sum();
        double matchedRatio = round1(matchedRatioRaw);

        List<NutritionDtos.MealNutritionItemResponse> withShare = calibrated.stream()
                .map(item -> copyWithShare(item, displayCalories))
                .toList();
        double safeMultiplier = Math.max(0.0, Math.min(2.0, consumptionMultiplier));
        int consumedCalories = (int) Math.round(providedCalories * safeMultiplier + addedCalories);
        double consumedCarb = consumedMacro(calibrated, safeMultiplier, NutritionDtos.MealNutritionItemResponse::getCarbohydrateG);
        double consumedProtein = consumedMacro(calibrated, safeMultiplier, NutritionDtos.MealNutritionItemResponse::getProteinG);
        double consumedFat = consumedMacro(calibrated, safeMultiplier, NutritionDtos.MealNutritionItemResponse::getFatG);

        return NutritionDtos.MealNutritionResponse.builder()
                .mealType(mealType)
                .officialCalorieKcal(officialCalorieKcal)
                .estimatedCalorieKcal(estimatedItemCalories)
                .matchedItemCount(matchedItemCount)
                .totalItemCount(totalItemCount)
                .matchedRatio(matchedRatio)
                .consumptionMultiplier(safeMultiplier)
                .consumedCalories(consumedCalories)
                .consumedCarbG(round1(consumedCarb))
                .consumedProteinG(round1(consumedProtein))
                .consumedFatG(round1(consumedFat))
                .items(withShare)
                .mealLabel(toMealLabel(mealType))
                .calories(displayCalories)
                .carbG(round1(carbohydrate))
                .proteinG(round1(protein))
                .fatG(round1(fat))
                .build();
    }

    private double calibrationScale(Integer official, int estimated, double matchedRatio) {
        if (official == null || estimated <= 0 || matchedRatio < 0.6) return 1.0;
        double scale = official / (double) estimated;
        return scale >= 0.7 && scale <= 1.3 ? scale : 1.0;
    }

    private NutritionDtos.MealNutritionItemResponse scaleItem(NutritionDtos.MealNutritionItemResponse item, double scale) {
        if (scale == 1.0 || item.getCalorieKcal() == null) return item;
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(item.getMenuName()).normalizedName(item.getNormalizedName()).matched(item.getMatched())
                .matchedFoodName(item.getMatchedFoodName()).displayCategory(item.getDisplayCategory())
                .matchType(item.getMatchType()).confidence(item.getConfidence()).servingGram(item.getServingGram())
                .calorieKcal((int) Math.round(item.getCalorieKcal() * scale))
                .carbohydrateG(scale(item.getCarbohydrateG(), scale)).proteinG(scale(item.getProteinG(), scale)).fatG(scale(item.getFatG(), scale))
                .foodId(item.getFoodId()).foodName(item.getFoodName()).mealType(item.getMealType()).category(item.getCategory())
                .servingUnit(item.getServingUnit()).calories((int) Math.round(item.getCalorieKcal() * scale))
                .carbG(scale(item.getCarbG(), scale)).addedByUser(item.getAddedByUser()).matchStatus(item.getMatchStatus()).build();
    }

    private Double scale(Double value, double multiplier) {
        return value == null ? null : round1(value * multiplier);
    }

    private double consumedMacro(List<NutritionDtos.MealNutritionItemResponse> items, double multiplier,
                                 java.util.function.Function<NutritionDtos.MealNutritionItemResponse, Double> getter) {
        return items.stream().mapToDouble(item -> Optional.ofNullable(getter.apply(item)).orElse(0.0)
                * (Boolean.TRUE.equals(item.getAddedByUser()) ? 1.0 : multiplier)).sum();
    }

    public List<String> parseMealItems(String rawMenu) {
        if (rawMenu == null || rawMenu.isBlank()) {
            return List.of();
        }
        return mealMenuItemParser.parse(rawMenu);
    }

    private NutritionDtos.MealNutritionItemResponse copyWithShare(NutritionDtos.MealNutritionItemResponse item, int mealCalories) {
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(item.getMenuName())
                .normalizedName(item.getNormalizedName())
                .matched(item.getMatched())
                .matchedFoodName(item.getMatchedFoodName())
                .displayCategory(item.getDisplayCategory())
                .matchType(item.getMatchType())
                .confidence(item.getConfidence())
                .servingGram(item.getServingGram())
                .calorieKcal(item.getCalorieKcal())
                .carbohydrateG(item.getCarbohydrateG())
                .proteinG(item.getProteinG())
                .fatG(item.getFatG())
                .foodId(item.getFoodId())
                .foodName(item.getFoodName())
                .mealType(item.getMealType())
                .category(item.getCategory())
                .servingUnit(item.getServingUnit())
                .calories(item.getCalories())
                .carbG(item.getCarbG())
                .calorieSharePct(percent(Optional.ofNullable(item.getCalorieKcal()).orElse(0), mealCalories))
                .addedByUser(item.getAddedByUser())
                .matchStatus(item.getMatchStatus())
                .build();
    }

    private Double percent(double intake, double target) {
        if (target <= 0) {
            return 0.0;
        }
        return round1(Math.min(100.0, Math.max(0.0, intake / target * 100.0)));
    }

    private String toMealLabel(String mealType) {
        return switch (mealType) {
            case "breakfast" -> "아침";
            case "lunch" -> "점심";
            case "dinner" -> "저녁";
            case "snack" -> "간식";
            default -> mealType;
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record CachedItem(NutritionDtos.MealNutritionItemResponse item, long createdAtMillis) {
    }
}
