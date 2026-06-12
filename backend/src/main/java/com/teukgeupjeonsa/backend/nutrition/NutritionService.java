package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import com.teukgeupjeonsa.backend.px.PxProduct;
import com.teukgeupjeonsa.backend.px.PxProductRepository;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import com.teukgeupjeonsa.backend.user.GoalType;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NutritionService {

    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;
    private final MealMenuRepository mealMenuRepository;
    private final UserOwnedFoodRepository userOwnedFoodRepository;
    private final UserMealFoodRepository userMealFoodRepository;
    private final FoodRepository foodRepository;
    private final PxProductRepository pxProductRepository;
    private final MealNutritionService mealNutritionService;
    private final FoodSearchService foodSearchService;


    @Transactional(readOnly = true)
    public NutritionDtos.NutritionSummaryResponse getTodaySummary(Long userId) {
        User user = getUser(userId);
        Optional<MealMenu> mealMenu = getTodayMealMenuOptional(user);

        Macro target = calculateTarget(user);
        List<UserMealFood> addedFoods = userMealFoodRepository.findByUserAndMealDate(user, LocalDate.now());
        Macro intake = calculateTodayIntake(mealMenu, addedFoods);

        return toSummary(target, intake, mealMenu.isPresent() || !addedFoods.isEmpty());
    }

    @Transactional(readOnly = true)
    public NutritionDtos.RecommendationResponse getTodayRecommendation(Long userId) {
        User user = getUser(userId);
        NutritionDtos.NutritionSummaryResponse summary = getTodaySummary(userId);

        double proteinDeficit = summary.getDeficitProteinG();
        List<String> owned = new ArrayList<>();
        List<String> px = new ArrayList<>();

        if (proteinDeficit > 0.1) {
            for (UserOwnedFood food : userOwnedFoodRepository.findByUser(user)) {
                if (food.getProteinG() != null && food.getProteinG() > 0) {
                    owned.add(String.format("%s %d개 (단백질 %.1fg)", food.getFoodName(), food.getQuantity(), food.getProteinG()));
                }
            }
            for (PxProduct product : pxProductRepository.findByIsActiveTrue()) {
                if (product.getProteinG() != null && product.getProteinG() >= 10) {
                    px.add(String.format("%s (단백질 %.1fg)", product.getProductName(), product.getProteinG()));
                }
            }
        }

        String text;
        if (summary.getIntakeCalories() <= 0) {
            text = "오늘 식단 데이터가 없어 추정 섭취량을 0으로 표시했습니다. 부대 식단을 먼저 동기화해주세요.";
        } else if (proteinDeficit <= 0) {
            text = "단백질 목표를 충족했습니다. 남은 탄수화물/지방 비율만 맞추면 좋습니다.";
        } else {
            text = String.format("단백질 %.1fg 부족. 보유식품 우선 사용 후 PX 보충을 권장합니다.", proteinDeficit);
        }

        return NutritionDtos.RecommendationResponse.builder()
                .summary(summary)
                .ownedFoodSuggestions(owned.stream().limit(3).toList())
                .pxSuggestions(px.stream().limit(3).toList())
                .recommendationText(text)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NutritionDtos.OwnedFoodResponse> getOwnedFoods(Long userId) {
        User user = getUser(userId);
        return userOwnedFoodRepository.findByUser(user).stream().map(this::toOwnedResponse).toList();
    }

    @Transactional(readOnly = true)
    public NutritionDtos.FoodSearchResponse searchFoods(String query) {
        return foodSearchService.search(query);
    }

    @Transactional
    public List<NutritionDtos.FoodNutritionItemResponse> addMealFoods(Long userId, NutritionDtos.AddMealFoodsRequest request) {
        User user = getUser(userId);
        String mealType = normalizeMealType(request.getMealType());
        List<Long> foodIds = Optional.ofNullable(request.getFoodIds()).orElse(List.of());
        if (foodIds.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, Double> servingGramByFoodId = Optional.ofNullable(request.getServingGramByFoodId()).orElse(java.util.Map.of());

        return foodRepository.findAllById(foodIds).stream()
                .map(food -> {
                    double servingGram = Math.max(0.0, Optional.ofNullable(servingGramByFoodId.get(food.getId())).orElse(100.0));
                    double scale = servingGram / 100.0;
                    return userMealFoodRepository.save(UserMealFood.builder()
                            .user(user)
                            .food(food)
                            .mealDate(LocalDate.now())
                            .mealType(mealType)
                            .foodName(food.getName())
                            .calories(toInt(multiply(food.getCalorie(), scale)))
                            .proteinG(multiply(food.getProtein(), scale))
                            .carbG(multiply(food.getCarbohydrate(), scale))
                            .fatG(multiply(food.getFat(), scale))
                            .quantity(servingGram)
                            .build());
                })
                .map(added -> toAddedFoodItem(added, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public NutritionDtos.TodayMealNutritionResponse getTodayMealDetails(Long userId) {
        User user = getUser(userId);
        Optional<MealMenu> mealMenu = getTodayMealMenuOptional(user);
        List<UserMealFood> addedFoods = userMealFoodRepository.findByUserAndMealDate(user, LocalDate.now());

        List<NutritionDtos.MealNutritionResponse> meals = List.of(
                buildMealDetail("breakfast", mealMenu.map(MealMenu::getBreakfast).orElse(null), mealMenu.map(MealMenu::getBreakfastKcal).orElse(null), addedFoods),
                buildMealDetail("lunch", mealMenu.map(MealMenu::getLunch).orElse(null), mealMenu.map(MealMenu::getLunchKcal).orElse(null), addedFoods),
                buildMealDetail("dinner", mealMenu.map(MealMenu::getDinner).orElse(null), mealMenu.map(MealMenu::getDinnerKcal).orElse(null), addedFoods),
                buildMealDetail("snack", null, null, addedFoods)
        );

        int totalCalories = meals.stream().mapToInt(meal -> Optional.ofNullable(meal.getCalories()).orElse(0)).sum();
        int totalOfficialCalories = meals.stream().mapToInt(meal -> Optional.ofNullable(meal.getOfficialCalorieKcal()).orElse(0)).sum();
        double totalProtein = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getProteinG()).orElse(0.0)).sum();
        double totalCarb = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getCarbG()).orElse(0.0)).sum();
        double totalFat = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getFatG()).orElse(0.0)).sum();

        return NutritionDtos.TodayMealNutritionResponse.builder()
                .totalCalories(totalCalories)
                .totalOfficialCalories(totalOfficialCalories)
                .totalEstimatedCalories(totalCalories)
                .totalProteinG(round1(totalProtein))
                .totalCarbG(round1(totalCarb))
                .totalFatG(round1(totalFat))
                .meals(meals)
                .build();
    }

    @Transactional
    public NutritionDtos.OwnedFoodResponse saveOwnedFood(Long userId, NutritionDtos.SaveOwnedFoodRequest request) {
        User user = getUser(userId);
        UserOwnedFood food = UserOwnedFood.builder()
                .user(user)
                .foodName(request.getFoodName())
                .calories(request.getCalories())
                .proteinG(request.getProteinG())
                .carbG(request.getCarbG())
                .fatG(request.getFatG())
                .quantity(request.getQuantity() == null ? 1 : request.getQuantity())
                .build();
        return toOwnedResponse(userOwnedFoodRepository.save(food));
    }

    @Transactional
    public NutritionDtos.OwnedFoodResponse updateOwnedFood(Long userId, Long id, NutritionDtos.SaveOwnedFoodRequest request) {
        User user = getUser(userId);
        UserOwnedFood food = userOwnedFoodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("보유 식품을 찾을 수 없습니다."));
        if (!food.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("다른 사용자의 식품입니다.");
        }
        food.setFoodName(request.getFoodName());
        food.setCalories(request.getCalories());
        food.setProteinG(request.getProteinG());
        food.setCarbG(request.getCarbG());
        food.setFatG(request.getFatG());
        food.setQuantity(request.getQuantity());
        return toOwnedResponse(food);
    }

    @Transactional
    public void deleteOwnedFood(Long userId, Long id) {
        User user = getUser(userId);
        UserOwnedFood food = userOwnedFoodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("보유 식품을 찾을 수 없습니다."));
        if (!food.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("다른 사용자의 식품입니다.");
        }
        userOwnedFoodRepository.delete(food);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Optional<MealMenu> getTodayMealMenuOptional(User user) {
        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .flatMap(setting -> {
                    String serviceCode = setting.getUnit().getDataSourceKey();
                    if (serviceCode == null || serviceCode.isBlank()) {
                        return Optional.empty();
                    }
                    return mealMenuRepository.findTopByServiceCodeAndMealDateOrderByUpdatedAtDesc(serviceCode, LocalDate.now());
                });
    }

    private Macro calculateTarget(User user) {
        GoalType goal = user.getGoalType() == null ? GoalType.GENERAL_FITNESS : user.getGoalType();

        if (user.getTargetWeight() == null || user.getHeightCm() == null) {
            return new Macro(0, 0, 0, 0);
        }

        double weight = user.getTargetWeight();
        double height = user.getHeightCm();

        // 나이/성별 정보가 없어 군인 기본값(남성 22세) 기반 Mifflin-St Jeor 근사 사용
        double bmr = 10 * weight + 6.25 * height - 5 * 22 + 5;

        int workoutDays = Optional.ofNullable(user.getWorkoutDaysPerWeek()).orElse(0);
        int preferredMinutes = Optional.ofNullable(user.getPreferredWorkoutMinutes()).orElse(0);
        double durationBoost = preferredMinutes >= 70 ? 0.08 : preferredMinutes >= 50 ? 0.04 : 0.0;
        double activityFactor = (workoutDays <= 2 ? 1.35 : workoutDays <= 4 ? 1.5 : 1.65) + durationBoost;
        double tdee = bmr * activityFactor;

        double targetCalories = switch (goal) {
            case BULK -> tdee + 300;
            case CUT -> tdee - 400;
            case FITNESS_TEST -> tdee;
            case MAINTAIN, GENERAL_FITNESS -> tdee;
        };

        if (targetCalories < 0) {
            targetCalories = 0;
        }

        double proteinPerKg = switch (goal) {
            case BULK -> 2.0;
            case CUT -> 2.2;
            case FITNESS_TEST -> 1.8;
            case MAINTAIN, GENERAL_FITNESS -> user.getWorkoutLevel() == com.teukgeupjeonsa.backend.user.WorkoutLevel.INTERMEDIATE ? 2.0 : 1.8;
        };

        double fatPerKg = switch (goal) {
            case BULK -> 0.9;
            case CUT -> 0.7;
            case FITNESS_TEST -> 0.8;
            case MAINTAIN, GENERAL_FITNESS -> 0.8;
        };

        double protein = weight * proteinPerKg;
        double fat = weight * fatPerKg;
        double carb = Math.max(0, (targetCalories - (protein * 4 + fat * 9)) / 4);

        return new Macro((int) Math.round(targetCalories), protein, carb, fat);
    }

    private Macro calculateTodayIntake(Optional<MealMenu> mealMenu, List<UserMealFood> addedFoods) {
        Macro base = mealMenu.map(menu -> add(
                add(estimateMealNutrition(menu.getBreakfast(), menu.getBreakfastKcal()), estimateMealNutrition(menu.getLunch(), menu.getLunchKcal())),
                estimateMealNutrition(menu.getDinner(), menu.getDinnerKcal())
        )).orElseGet(() -> new Macro(0, 0, 0, 0));

        Macro added = addedFoods.stream()
                .map(food -> new Macro(Optional.ofNullable(food.getCalories()).orElse(0), nvl(food.getProteinG()), nvl(food.getCarbG()), nvl(food.getFatG())))
                .reduce(new Macro(0, 0, 0, 0), this::add);
        return add(base, added);
    }

    private NutritionDtos.MealNutritionResponse buildMealDetail(String mealType, String rawMenu, Integer rawKcal, List<UserMealFood> addedFoods) {
        NutritionDtos.MealNutritionResponse baseMeal = mealNutritionService.analyzeMeal(mealType, rawMenu, rawKcal);
        List<NutritionDtos.MealNutritionItemResponse> items = new ArrayList<>(baseMeal.getItems());
        addedFoods.stream()
                .filter(food -> mealType.equals(food.getMealType()))
                .map(food -> toAddedMealNutritionItem(food, mealType))
                .forEach(items::add);
        return mealNutritionService.buildResponse(mealType, rawKcal, items);
    }

    private Macro estimateMealNutrition(String rawMenu, Integer rawKcal) {
        NutritionDtos.MealNutritionResponse meal = mealNutritionService.analyzeMeal("", rawMenu, rawKcal);
        Macro calculated = meal.getItems().stream()
                .map(item -> new Macro(Optional.ofNullable(item.getCalorieKcal()).orElse(0), nvl(item.getProteinG()), nvl(item.getCarbohydrateG()), nvl(item.getFatG())))
                .reduce(new Macro(0, 0, 0, 0), this::add);
        return new Macro(Optional.ofNullable(rawKcal).orElse(calculated.calories), calculated.protein, calculated.carb, calculated.fat);
    }

    private NutritionDtos.NutritionSummaryResponse toSummary(Macro target, Macro intake, boolean hasMealData) {
        int remainingCalories = Math.max(0, target.calories - intake.calories);
        double remainingProtein = Math.max(0, target.protein - intake.protein);
        double remainingCarb = Math.max(0, target.carb - intake.carb);
        double remainingFat = Math.max(0, target.fat - intake.fat);

        return NutritionDtos.NutritionSummaryResponse.builder()
                .targetCalories(target.calories)
                .targetProteinG(round1(target.protein))
                .targetCarbG(round1(target.carb))
                .targetFatG(round1(target.fat))
                .intakeCalories(intake.calories)
                .intakeProteinG(round1(intake.protein))
                .intakeCarbG(round1(intake.carb))
                .intakeFatG(round1(intake.fat))
                .remainingCalories(remainingCalories)
                .remainingProteinG(round1(remainingProtein))
                .remainingCarbG(round1(remainingCarb))
                .remainingFatG(round1(remainingFat))
                .calorieProgressPct(percent(intake.calories, target.calories))
                .proteinProgressPct(percent(intake.protein, target.protein))
                .carbProgressPct(percent(intake.carb, target.carb))
                .fatProgressPct(percent(intake.fat, target.fat))
                .deficitProteinG(round1(remainingProtein))
                .deficitCarbG(round1(remainingCarb))
                .deficitFatG(round1(remainingFat))
                .note(hasMealData
                        ? "군 급식 메뉴와 식품 DB를 기반으로 오늘 추정 섭취량을 계산했어요. 실제 조리법과 배식량에 따라 차이가 있을 수 있습니다."
                        : "당일 식단 데이터가 없어 추정 섭취량은 0으로 표시됩니다.")
                .build();
    }

    private double percent(double intake, double target) {
        if (target <= 0) {
            return 0;
        }
        double pct = (intake / target) * 100.0;
        return round1(Math.min(100, Math.max(0, pct)));
    }

    private NutritionDtos.MealNutritionItemResponse toAddedMealNutritionItem(UserMealFood food, String mealType) {
        Integer calories = food.getCalories();
        Double carb = food.getCarbG();
        Double protein = food.getProteinG();
        Double fat = food.getFatG();
        return NutritionDtos.MealNutritionItemResponse.builder()
                .menuName(food.getFoodName())
                .normalizedName(food.getFoodName())
                .matched(true)
                .matchedFoodName(food.getFoodName())
                .matchType("USER_ADDED")
                .confidence(MatchConfidence.HIGH)
                .servingGram(null)
                .calorieKcal(calories)
                .carbohydrateG(carb)
                .proteinG(protein)
                .fatG(fat)
                .foodId(food.getFood() == null ? null : food.getFood().getId())
                .foodName(food.getFoodName())
                .mealType(mealType)
                .category(food.getFood() == null ? null : food.getFood().getCategory())
                .servingUnit(food.getFood() == null ? null : food.getFood().getServingUnit())
                .calories(calories)
                .carbG(carb)
                .addedByUser(true)
                .matchStatus("ADDED")
                .build();
    }

    private NutritionDtos.FoodNutritionItemResponse toAddedFoodItem(UserMealFood food, int mealCalories) {
        return NutritionDtos.FoodNutritionItemResponse.builder()
                .id(food.getId())
                .foodId(food.getFood() == null ? null : food.getFood().getId())
                .foodName(food.getFoodName())
                .matchedFoodName(food.getFoodName())
                .mealType(food.getMealType())
                .category(food.getFood() == null ? null : food.getFood().getCategory())
                .servingUnit(food.getFood() == null ? null : food.getFood().getServingUnit())
                .calories(Optional.ofNullable(food.getCalories()).orElse(0))
                .proteinG(round1(nvl(food.getProteinG())))
                .carbG(round1(nvl(food.getCarbG())))
                .fatG(round1(nvl(food.getFatG())))
                .calorieSharePct(percent(Optional.ofNullable(food.getCalories()).orElse(0), mealCalories))
                .addedByUser(true)
                .matchStatus("ADDED")
                .build();
    }

    private NutritionDtos.FoodNutritionItemResponse copyWithShare(NutritionDtos.FoodNutritionItemResponse item, int mealCalories) {
        return NutritionDtos.FoodNutritionItemResponse.builder()
                .id(item.getId())
                .foodId(item.getFoodId())
                .foodName(item.getFoodName())
                .matchedFoodName(item.getMatchedFoodName())
                .mealType(item.getMealType())
                .category(item.getCategory())
                .servingUnit(item.getServingUnit())
                .calories(item.getCalories())
                .proteinG(item.getProteinG())
                .carbG(item.getCarbG())
                .fatG(item.getFatG())
                .calorieSharePct(percent(Optional.ofNullable(item.getCalories()).orElse(0), mealCalories))
                .addedByUser(item.getAddedByUser())
                .matchStatus(item.getMatchStatus())
                .build();
    }

    private String normalizeMealType(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "breakfast", "morning", "아침", "조식" -> "breakfast";
            case "lunch", "noon", "점심", "중식" -> "lunch";
            case "dinner", "evening", "저녁", "석식" -> "dinner";
            case "snack", "간식" -> "snack";
            default -> throw new IllegalArgumentException("mealType은 breakfast/lunch/dinner/snack 중 하나여야 합니다.");
        };
    }

    private Macro add(Macro first, Macro second) {
        return new Macro(first.calories + second.calories, first.protein + second.protein, first.carb + second.carb, first.fat + second.fat);
    }

    private int toInt(Double value) {
        return value == null ? 0 : (int) Math.round(value);
    }

    private double nvl(Double value) {
        return value == null ? 0.0 : value;
    }

    private NutritionDtos.OwnedFoodResponse toOwnedResponse(UserOwnedFood food) {
        return NutritionDtos.OwnedFoodResponse.builder()
                .id(food.getId())
                .foodName(food.getFoodName())
                .calories(food.getCalories())
                .proteinG(food.getProteinG())
                .carbG(food.getCarbG())
                .fatG(food.getFatG())
                .quantity(food.getQuantity())
                .build();
    }


    private Double multiply(Double value, double scale) {
        return value == null ? null : round1(value * scale);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Macro(int calories, double protein, double carb, double fat) {
    }
}
