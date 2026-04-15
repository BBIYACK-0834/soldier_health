package com.teukgeupjeonsa.backend.mealcrawler.repository;

import com.teukgeupjeonsa.backend.mealcrawler.entity.MealMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealMenuRepository extends JpaRepository<MealMenu, Long> {
    Optional<MealMenu> findByInfIdAndMealDate(String infId, LocalDate mealDate);
}
