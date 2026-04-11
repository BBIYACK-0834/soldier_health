package com.teukgeupjeonsa.backend.nutrition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodNutritionRepository extends JpaRepository<FoodNutrition, Long> {
    List<FoodNutrition> findByFoodNameIn(List<String> foodNames);
}
