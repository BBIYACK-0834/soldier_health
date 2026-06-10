package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import org.springframework.stereotype.Component;

@Component
public class NutritionCalculator {

    public CalculatedNutrition calculate(FoodMatchResult match, double servingGram) {
        if (match == null || !match.isMatched() || match.getMatchedFood() == null) {
            return new CalculatedNutrition(null, null, null, null);
        }

        Food food = match.getMatchedFood();
        double scale = servingGram / 100.0;
        return new CalculatedNutrition(
                multiply(food.getCalorie(), scale),
                multiply(food.getCarbohydrate(), scale),
                multiply(food.getProtein(), scale),
                multiply(food.getFat(), scale)
        );
    }

    private Double multiply(Double value, double scale) {
        if (value == null) {
            return null;
        }
        return round1(value * scale);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record CalculatedNutrition(Double calorieKcal, Double carbohydrateG, Double proteinG, Double fatG) {
    }
}
