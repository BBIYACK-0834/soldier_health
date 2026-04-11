package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.meal.MealDay;
import com.teukgeupjeonsa.backend.meal.MealDayRepository;
import com.teukgeupjeonsa.backend.px.PxProduct;
import com.teukgeupjeonsa.backend.px.PxProductRepository;
import com.teukgeupjeonsa.backend.unit.UserUnitSetting;
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
        MealDay mealDay = getTodayMeal(user);

        Macro target = calculateTarget(user);
        Macro intake = estimateMealNutrition(mealDay);

        return toSummary(target, intake);
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

        String text = proteinDeficit <= 0
                ? "오늘 단백질은 목표를 충족했습니다. 수분 보충과 규칙적인 식사를 유지하세요."
                : String.format("단백질 %.1fg 부족 예상. 보유식품 우선 사용 후 PX 보충을 권장합니다.", proteinDeficit);

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

    private MealDay getTodayMeal(User user) {
        UserUnitSetting setting = userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new EntityNotFoundException("부대 설정이 필요합니다."));
        return mealDayRepository.findByUnitAndMealDate(setting.getUnit(), LocalDate.now())
                .orElseThrow(() -> new EntityNotFoundException("오늘 식단이 없습니다."));
    }

    private Macro calculateTarget(User user) {
        double weight = user.getWeightKg() == null ? 70.0 : user.getWeightKg();
        double proteinPerKg = switch (user.getGoalType()) {
            case BULK -> 2.0;
            case CUT -> 2.2;
            case MAINTAIN, GENERAL_FITNESS -> 1.8;
            case FITNESS_TEST -> 1.6;
        };
        double protein = weight * proteinPerKg;
        int calories = switch (user.getGoalType()) {
            case BULK -> (int) (weight * 36);
            case CUT -> (int) (weight * 30);
            case MAINTAIN, GENERAL_FITNESS -> (int) (weight * 33);
            case FITNESS_TEST -> (int) (weight * 32);
        };
        double fat = weight * 0.8;
        double carb = (calories - (protein * 4 + fat * 9)) / 4;
        return new Macro(calories, protein, carb, fat);
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

    private NutritionDtos.NutritionSummaryResponse toSummary(Macro target, Macro intake) {
        return NutritionDtos.NutritionSummaryResponse.builder()
                .targetCalories(target.calories)
                .targetProteinG(round1(target.protein))
                .targetCarbG(round1(target.carb))
                .targetFatG(round1(target.fat))
                .intakeCalories(intake.calories)
                .intakeProteinG(round1(intake.protein))
                .intakeCarbG(round1(intake.carb))
                .intakeFatG(round1(intake.fat))
                .deficitProteinG(round1(Math.max(0, target.protein - intake.protein)))
                .deficitCarbG(round1(Math.max(0, target.carb - intake.carb)))
                .deficitFatG(round1(Math.max(0, target.fat - intake.fat)))
                .note("식단 텍스트 기반 추정치입니다.")
                .build();
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

    private record Macro(int calories, double protein, double carb, double fat) {}
}
