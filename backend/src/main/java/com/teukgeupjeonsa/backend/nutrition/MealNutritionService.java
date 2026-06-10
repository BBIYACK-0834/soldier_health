package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MealNutritionService {

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

    public NutritionDtos.MealNutritionResponse analyzeMeal(String mealType, String rawMenu, Integer officialCalorieKcal) {
        return analyzeMeal(mealType, parseMealItems(rawMenu), officialCalorieKcal);
    }

    public NutritionDtos.MealNutritionResponse analyzeMeal(String mealType, List<String> menuNames, Integer officialCalorieKcal) {
        List<NutritionDtos.MealNutritionItemResponse> items = new ArrayList<>();
        for (String menuName : menuNames) {
            if (menuName == null || menuName.isBlank()) {
                continue;
            }
            items.add(analyzeItem(mealType, menuName));
        }
        return buildResponse(mealType, officialCalorieKcal, items);
    }

    public NutritionDtos.MealNutritionResponse buildSampleMealNutritionResponse() {
        return analyzeMeal("sample", SAMPLE_MENUS, 900);
    }

    private NutritionDtos.MealNutritionItemResponse analyzeItem(String mealType, String menuName) {
        String normalizedName = foodNameNormalizer.normalize(menuName);
        FoodMatchResult match = foodMatcher.match(menuName);
        Food food = match.getMatchedFood();
        double servingGram = servingEstimator.estimateGram(normalizedName, food == null ? null : food.getCategory());
        NutritionCalculator.CalculatedNutrition nutrition = nutritionCalculator.calculate(match, servingGram);
        Integer calorieKcal = nutrition.calorieKcal() == null ? null : (int) Math.round(nutrition.calorieKcal());
        String matchStatus = match.isMatched() ? "MATCHED" : "UNMATCHED";

        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(menuName)
                .normalizedName(normalizedName)
                .matched(match.isMatched())
                .matchedFoodName(match.getMatchedFoodName())
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
                .matchStatus(matchStatus)
                .build();
    }

    public NutritionDtos.MealNutritionResponse buildResponse(String mealType, Integer officialCalorieKcal, List<NutritionDtos.MealNutritionItemResponse> items) {
        int matchedItemCount = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getMatched())).count();
        int totalItemCount = items.size();
        int estimatedCalorieKcal = items.stream()
                .map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        double carbohydrate = items.stream().mapToDouble(item -> Optional.ofNullable(item.getCarbohydrateG()).orElse(0.0)).sum();
        double protein = items.stream().mapToDouble(item -> Optional.ofNullable(item.getProteinG()).orElse(0.0)).sum();
        double fat = items.stream().mapToDouble(item -> Optional.ofNullable(item.getFatG()).orElse(0.0)).sum();
        double matchedRatio = totalItemCount == 0 ? 0.0 : round1(matchedItemCount / (double) totalItemCount);

        List<NutritionDtos.MealNutritionItemResponse> withShare = items.stream()
                .map(item -> copyWithShare(item, estimatedCalorieKcal))
                .toList();

        return NutritionDtos.MealNutritionResponse.builder()
                .mealType(mealType)
                .officialCalorieKcal(officialCalorieKcal)
                .estimatedCalorieKcal(estimatedCalorieKcal)
                .matchedItemCount(matchedItemCount)
                .totalItemCount(totalItemCount)
                .matchedRatio(matchedRatio)
                .items(withShare)
                .mealLabel(toMealLabel(mealType))
                .calories(estimatedCalorieKcal)
                .carbG(round1(carbohydrate))
                .proteinG(round1(protein))
                .fatG(round1(fat))
                .build();
    }

    public List<String> parseMealItems(String rawMenu) {
        if (rawMenu == null || rawMenu.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(rawMenu.split("[,/\\n]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private NutritionDtos.MealNutritionItemResponse copyWithShare(NutritionDtos.MealNutritionItemResponse item, int mealCalories) {
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(item.getMenuName())
                .normalizedName(item.getNormalizedName())
                .matched(item.getMatched())
                .matchedFoodName(item.getMatchedFoodName())
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
}
