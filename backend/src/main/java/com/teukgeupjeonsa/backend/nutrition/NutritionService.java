package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NutritionService {

    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;
    private final MealMenuRepository mealMenuRepository;
    private final UserOwnedFoodRepository userOwnedFoodRepository;
    private final UserMealFoodRepository userMealFoodRepository;
    private final FoodRepository foodRepository;
    private final FoodAliasRepository foodAliasRepository;
    private final PxProductRepository pxProductRepository;

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
            text = "오늘 식단 데이터가 없어 섭취량을 0으로 계산했습니다. 부대 식단을 먼저 동기화해주세요.";
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
    public List<NutritionDtos.FoodSearchResponse> searchFoods(String query) {
        String keyword = Optional.ofNullable(query).orElse("").trim();
        if (keyword.isBlank()) {
            return List.of();
        }

        String normalizedKeyword = normalizeSearchName(keyword);
        Map<Long, NutritionDtos.FoodSearchResponse> results = new LinkedHashMap<>();

        foodAliasRepository.findByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCase(
                        keyword, normalizedKeyword, keyword, PageRequest.of(0, 12)
                )
                .forEach(alias -> results.putIfAbsent(alias.getFood().getId(), toFoodSearchResponse(alias.getFood(), alias.getOriginalName())));

        foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(keyword, normalizedKeyword, PageRequest.of(0, 12))
                .forEach(food -> results.putIfAbsent(food.getId(), toFoodSearchResponse(food, food.getName())));

        return results.values().stream().limit(20).toList();
    }

    @Transactional
    public List<NutritionDtos.FoodNutritionItemResponse> addMealFoods(Long userId, NutritionDtos.AddMealFoodsRequest request) {
        User user = getUser(userId);
        String mealType = normalizeMealType(request.getMealType());
        List<Long> foodIds = Optional.ofNullable(request.getFoodIds()).orElse(List.of());
        if (foodIds.isEmpty()) {
            return List.of();
        }

        return foodRepository.findAllById(foodIds).stream()
                .map(food -> userMealFoodRepository.save(UserMealFood.builder()
                        .user(user)
                        .food(food)
                        .mealDate(LocalDate.now())
                        .mealType(mealType)
                        .foodName(food.getName())
                        .calories(toInt(food.getCalorie()))
                        .proteinG(nvl(food.getProtein()))
                        .carbG(nvl(food.getCarbohydrate()))
                        .fatG(nvl(food.getFat()))
                        .quantity(1.0)
                        .build()))
                .map(added -> toAddedFoodItem(added, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public NutritionDtos.TodayMealNutritionResponse getTodayMealDetails(Long userId) {
        User user = getUser(userId);
        Optional<MealMenu> mealMenu = getTodayMealMenuOptional(user);
        List<UserMealFood> addedFoods = userMealFoodRepository.findByUserAndMealDate(user, LocalDate.now());

        List<NutritionDtos.MealNutritionDetailResponse> meals = List.of(
                buildMealDetail("breakfast", "아침", mealMenu.map(MealMenu::getBreakfast).orElse(null), mealMenu.map(MealMenu::getBreakfastKcal).orElse(null), addedFoods),
                buildMealDetail("lunch", "점심", mealMenu.map(MealMenu::getLunch).orElse(null), mealMenu.map(MealMenu::getLunchKcal).orElse(null), addedFoods),
                buildMealDetail("dinner", "저녁", mealMenu.map(MealMenu::getDinner).orElse(null), mealMenu.map(MealMenu::getDinnerKcal).orElse(null), addedFoods),
                buildMealDetail("snack", "간식", null, null, addedFoods)
        );

        int totalCalories = meals.stream().mapToInt(meal -> Optional.ofNullable(meal.getCalories()).orElse(0)).sum();
        double totalProtein = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getProteinG()).orElse(0.0)).sum();
        double totalCarb = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getCarbG()).orElse(0.0)).sum();
        double totalFat = meals.stream().mapToDouble(meal -> Optional.ofNullable(meal.getFatG()).orElse(0.0)).sum();

        return NutritionDtos.TodayMealNutritionResponse.builder()
                .totalCalories(totalCalories)
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

        if (user.getWeightKg() == null || user.getHeightCm() == null) {
            return new Macro(0, 0, 0, 0);
        }

        double weight = user.getWeightKg();
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
        Macro base = mealMenu.map(menu -> {
            Macro breakfast = estimateMealNutrition(menu.getBreakfast(), menu.getBreakfastKcal());
            Macro lunch = estimateMealNutrition(menu.getLunch(), menu.getLunchKcal());
            Macro dinner = estimateMealNutrition(menu.getDinner(), menu.getDinnerKcal());
            return add(add(breakfast, lunch), dinner);
        }).orElseGet(() -> new Macro(0, 0, 0, 0));

        Macro added = addedFoods.stream()
                .map(food -> new Macro(Optional.ofNullable(food.getCalories()).orElse(0), nvl(food.getProteinG()), nvl(food.getCarbG()), nvl(food.getFatG())))
                .reduce(new Macro(0, 0, 0, 0), this::add);
        return add(base, added);
    }

    private NutritionDtos.MealNutritionDetailResponse buildMealDetail(String mealType, String mealLabel, String rawMenu, Integer rawKcal, List<UserMealFood> addedFoods) {
        List<NutritionDtos.FoodNutritionItemResponse> items = new ArrayList<>(estimateMealItems(mealType, rawMenu, rawKcal));
        addedFoods.stream()
                .filter(food -> mealType.equals(food.getMealType()))
                .map(food -> toAddedFoodItem(food, 0))
                .forEach(items::add);

        int calories = items.stream().mapToInt(item -> Optional.ofNullable(item.getCalories()).orElse(0)).sum();
        double protein = items.stream().mapToDouble(item -> Optional.ofNullable(item.getProteinG()).orElse(0.0)).sum();
        double carb = items.stream().mapToDouble(item -> Optional.ofNullable(item.getCarbG()).orElse(0.0)).sum();
        double fat = items.stream().mapToDouble(item -> Optional.ofNullable(item.getFatG()).orElse(0.0)).sum();

        List<NutritionDtos.FoodNutritionItemResponse> withShare = items.stream()
                .map(item -> copyWithShare(item, calories))
                .toList();

        return NutritionDtos.MealNutritionDetailResponse.builder()
                .mealType(mealType)
                .mealLabel(mealLabel)
                .calories(calories)
                .proteinG(round1(protein))
                .carbG(round1(carb))
                .fatG(round1(fat))
                .items(withShare)
                .build();
    }

    private List<NutritionDtos.FoodNutritionItemResponse> estimateMealItems(String mealType, String rawMenu, Integer rawKcal) {
        List<String> names = parseMealItems(rawMenu);
        if (names.isEmpty()) {
            return List.of();
        }

        List<MatchedFood> matches = names.stream().map(this::matchFood).toList();
        List<Double> calorieWeights = matches.stream()
                .map(match -> match.food().map(food -> Math.max(nvl(food.getCalorie()), 0.0)).orElse(0.0))
                .toList();
        double knownCalories = calorieWeights.stream().mapToDouble(Double::doubleValue).sum();
        double fallbackWeight = calorieWeights.stream()
                .filter(value -> value > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
        List<Double> allocationWeights = calorieWeights.stream()
                .map(value -> value > 0 ? value : fallbackWeight)
                .toList();
        double totalAllocationWeight = allocationWeights.stream().mapToDouble(Double::doubleValue).sum();
        int targetMealCalories = rawKcal == null || rawKcal <= 0
                ? (int) Math.round(knownCalories)
                : rawKcal;

        List<NutritionDtos.FoodNutritionItemResponse> items = new ArrayList<>();
        int allocatedCalories = 0;
        for (int index = 0; index < matches.size(); index++) {
            MatchedFood match = matches.get(index);
            Optional<Food> food = match.food();
            int calories;
            if (targetMealCalories > 0 && totalAllocationWeight > 0) {
                if (index == matches.size() - 1) {
                    calories = Math.max(0, targetMealCalories - allocatedCalories);
                } else {
                    calories = (int) Math.round(targetMealCalories * allocationWeights.get(index) / totalAllocationWeight);
                    allocatedCalories += calories;
                }
            } else {
                calories = food.map(value -> toInt(value.getCalorie())).orElse(0);
            }

            double scale = food.map(value -> {
                double originalCalories = nvl(value.getCalorie());
                return originalCalories > 0 ? calories / originalCalories : 1.0;
            }).orElse(0.0);

            items.add(NutritionDtos.FoodNutritionItemResponse.builder()
                    .foodId(food.map(Food::getId).orElse(null))
                    .foodName(match.originalName())
                    .matchedFoodName(food.map(Food::getName).orElse(null))
                    .mealType(mealType)
                    .category(food.map(Food::getCategory).orElse(null))
                    .servingUnit(food.map(Food::getServingUnit).orElse(null))
                    .calories(calories)
                    .proteinG(round1(food.map(value -> nvl(value.getProtein()) * scale).orElse(0.0)))
                    .carbG(round1(food.map(value -> nvl(value.getCarbohydrate()) * scale).orElse(0.0)))
                    .fatG(round1(food.map(value -> nvl(value.getFat()) * scale).orElse(0.0)))
                    .addedByUser(false)
                    .matchStatus(food.isPresent() ? "MATCHED" : "UNMATCHED")
                    .build());
        }

        return items;
    }

    private Macro estimateMealNutrition(String rawMenu, Integer rawKcal) {
        return estimateMealItems("", rawMenu, rawKcal).stream()
                .map(item -> new Macro(Optional.ofNullable(item.getCalories()).orElse(0), nvl(item.getProteinG()), nvl(item.getCarbG()), nvl(item.getFatG())))
                .reduce(new Macro(0, 0, 0, 0), this::add);
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
                        ? "엑셀 식품 DB와 직접 추가 음식을 기준으로 오늘 섭취량을 계산했어요."
                        : "당일 식단 데이터가 없어 섭취량은 0으로 계산되었습니다.")
                .build();
    }

    private double percent(double intake, double target) {
        if (target <= 0) {
            return 0;
        }
        double pct = (intake / target) * 100.0;
        return round1(Math.min(100, Math.max(0, pct)));
    }

    private MatchedFood matchFood(String foodName) {
        String originalName = Optional.ofNullable(foodName).orElse("").trim();
        if (originalName.isBlank()) {
            return new MatchedFood(originalName, Optional.empty());
        }

        for (String keyword : mealSearchVariants(originalName)) {
            String normalizedKeyword = normalizeSearchName(keyword);
            Optional<FoodAlias> alias = foodAliasRepository.findFirstByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseOrderByFood_SourceCountDesc(
                    keyword, normalizedKeyword, keyword
            );
            if (alias.isPresent()) {
                return new MatchedFood(originalName, Optional.of(alias.get().getFood()));
            }

            Optional<Food> food = foodRepository.findFirstByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrderBySourceCountDesc(keyword, normalizedKeyword);
            if (food.isPresent()) {
                return new MatchedFood(originalName, food);
            }
        }

        return new MatchedFood(originalName, findClosestFood(originalName));
    }

    private Optional<Food> findClosestFood(String foodName) {
        String cleaned = normalizeSearchName(cleanMealFoodName(foodName));
        if (cleaned.length() < 2) {
            return Optional.empty();
        }

        Set<Food> candidates = new HashSet<>();
        for (String variant : mealSearchVariants(foodName)) {
            String normalized = normalizeSearchName(variant);
            if (normalized.length() >= 2) {
                candidates.addAll(foodRepository.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(variant, normalized, PageRequest.of(0, 30)));
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .map(food -> new FoodMatchScore(food, similarityScore(cleaned, normalizeSearchName(food.getName()))))
                .filter(score -> score.score() >= 0.45)
                .max(Comparator.comparingDouble(FoodMatchScore::score)
                        .thenComparing(score -> Optional.ofNullable(score.food().getSourceCount()).orElse(0)))
                .map(FoodMatchScore::food);
    }

    private List<String> mealSearchVariants(String foodName) {
        String cleaned = cleanMealFoodName(foodName);
        String typoFixed = cleaned.replace("어그", "에그");
        String withoutModifiers = removeCommonMenuModifiers(typoFixed);

        LinkedHashMap<String, String> variants = new LinkedHashMap<>();
        for (String value : List.of(foodName, cleaned, typoFixed, withoutModifiers)) {
            String normalized = normalizeSearchName(value);
            if (!normalized.isBlank()) {
                variants.put(normalized, value.trim());
            }
        }
        return new ArrayList<>(variants.values());
    }

    private String cleanMealFoodName(String foodName) {
        return Optional.ofNullable(foodName).orElse("")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("[0-9]", "")
                .replaceAll("[★*•·]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeCommonMenuModifiers(String foodName) {
        return foodName
                .replace("치즈", "")
                .replace("에그", "")
                .replace("계란", "")
                .replace("더블", "")
                .replace("디럭스", "")
                .replace("스파이시", "")
                .replace("리얼", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double similarityScore(String source, String target) {
        if (source.isBlank() || target.isBlank()) {
            return 0;
        }
        if (target.contains(source) || source.contains(target)) {
            return Math.min(source.length(), target.length()) / (double) Math.max(source.length(), target.length());
        }

        int longest = longestCommonSubstring(source, target);
        double containment = longest / (double) Math.max(source.length(), target.length());
        double overlap = characterOverlap(source, target);
        return containment * 0.65 + overlap * 0.35;
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

    private NutritionDtos.FoodSearchResponse toFoodSearchResponse(Food food, String matchedName) {
        return NutritionDtos.FoodSearchResponse.builder()
                .id(food.getId())
                .foodName(food.getName())
                .category(food.getCategory())
                .servingUnit(food.getServingUnit())
                .calories(toInt(food.getCalorie()))
                .proteinG(round1(nvl(food.getProtein())))
                .carbG(round1(nvl(food.getCarbohydrate())))
                .fatG(round1(nvl(food.getFat())))
                .matchedName(matchedName)
                .build();
    }

    private List<String> parseMealItems(String rawMenu) {
        if (rawMenu == null || rawMenu.isBlank()) {
            return List.of();
        }
        return List.of(rawMenu.split("[,/\\n]")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String normalizeMealType(String mealType) {
        String normalized = Optional.ofNullable(mealType).orElse("snack").toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "breakfast", "lunch", "dinner", "snack" -> normalized;
            default -> "snack";
        };
    }

    private String normalizeSearchName(String value) {
        return Optional.ofNullable(value).orElse("").replaceAll("\\s+", "");
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

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Macro(int calories, double protein, double carb, double fat) {
    }

    private record MatchedFood(String originalName, Optional<Food> food) {
    }

    private record FoodMatchScore(Food food, double score) {
    }
}
