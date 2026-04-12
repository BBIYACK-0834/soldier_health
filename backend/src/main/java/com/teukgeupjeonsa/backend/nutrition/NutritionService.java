package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.meal.MealDay;
import com.teukgeupjeonsa.backend.meal.MealDayRepository;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class NutritionService {

    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;
    private final MealDayRepository mealDayRepository;
    private final FoodNutritionRepository foodNutritionRepository;
    private final UserOwnedFoodRepository userOwnedFoodRepository;
    private final PxProductRepository pxProductRepository;

    @Transactional(readOnly = true)
    public NutritionDtos.NutritionSummaryResponse getTodaySummary(Long userId) {
        User user = getUser(userId);
        Optional<MealDay> mealDay = getTodayMealOptional(user);

        Macro target = calculateTarget(user);
        Macro intake = mealDay.map(this::estimateMealNutrition).orElseGet(() -> new Macro(0, 0, 0, 0));

        return toSummary(target, intake, mealDay.isPresent());
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

    private Optional<MealDay> getTodayMealOptional(User user) {
        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .flatMap(setting -> mealDayRepository.findByUnitAndMealDate(setting.getUnit(), LocalDate.now()));
    }

    private Macro calculateTarget(User user) {
        GoalType goal = user.getGoalType() == null ? GoalType.GENERAL_FITNESS : user.getGoalType();

        double weight = Optional.ofNullable(user.getWeightKg()).orElse(70.0);
        double height = Optional.ofNullable(user.getHeightCm()).orElse(172.0);

        // 나이/성별 정보가 없어 군인 기본값(남성 22세) 기반 Mifflin-St Jeor 근사 사용
        double bmr = 10 * weight + 6.25 * height - 5 * 22 + 5;

        int workoutDays = Optional.ofNullable(user.getWorkoutDaysPerWeek()).orElse(3);
        double activityFactor = workoutDays <= 2 ? 1.35 : workoutDays <= 4 ? 1.5 : 1.65;
        double tdee = bmr * activityFactor;

        double targetCalories = switch (goal) {
            case BULK -> tdee + 300;
            case CUT -> tdee - 400;
            case FITNESS_TEST -> tdee;
            case MAINTAIN, GENERAL_FITNESS -> tdee;
        };

        if (targetCalories < 1500) {
            targetCalories = 1500;
        }

        double proteinPerKg = switch (goal) {
            case BULK -> 2.0;
            case CUT -> 2.2;
            case FITNESS_TEST -> 1.8;
            case MAINTAIN, GENERAL_FITNESS -> 1.8;
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

    private Macro estimateMealNutrition(MealDay mealDay) {
        String merged = String.join(",",
                Optional.ofNullable(mealDay.getBreakfastRaw()).orElse(""),
                Optional.ofNullable(mealDay.getLunchRaw()).orElse(""),
                Optional.ofNullable(mealDay.getDinnerRaw()).orElse(""));

        List<String> tokens = Arrays.stream(merged.split("[,/\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        Map<String, FoodNutrition> nutritionMap = new HashMap<>();
        for (FoodNutrition food : foodNutritionRepository.findAll()) {
            nutritionMap.put(food.getFoodName(), food);
        }

        int calories = 0;
        double protein = 0;
        double carb = 0;
        double fat = 0;

        for (String token : tokens) {
            FoodNutrition matched = nutritionMap.entrySet().stream()
                    .filter(entry -> token.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);

            if (matched != null) {
                calories += Optional.ofNullable(matched.getCalories()).orElse(0);
                protein += Optional.ofNullable(matched.getProteinG()).orElse(0.0);
                carb += Optional.ofNullable(matched.getCarbG()).orElse(0.0);
                fat += Optional.ofNullable(matched.getFatG()).orElse(0.0);
            }
        }

        if (calories == 0) {
            calories = Optional.ofNullable(mealDay.getBreakfastKcal()).orElse(0)
                    + Optional.ofNullable(mealDay.getLunchKcal()).orElse(0)
                    + Optional.ofNullable(mealDay.getDinnerKcal()).orElse(0);
        }

        return new Macro(calories, protein, carb, fat);
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
                        ? "당일 식단 + 보유 영양 DB 기반 추정치입니다."
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
}
