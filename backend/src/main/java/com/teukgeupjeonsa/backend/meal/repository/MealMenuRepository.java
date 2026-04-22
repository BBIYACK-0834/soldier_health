package com.teukgeupjeonsa.backend.meal.repository;

import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealMenuRepository extends JpaRepository<MealMenu, Long> {
    Optional<MealMenu> findBySourceNameAndMealDate(String sourceName, LocalDate mealDate);
    Optional<MealMenu> findByServiceCodeAndMealDate(String serviceCode, LocalDate mealDate);
    Optional<MealMenu> findTopByServiceCodeAndMealDateOrderByUpdatedAtDesc(String serviceCode, LocalDate mealDate);
    List<MealMenu> findByServiceCodeAndMealDateBetweenOrderByMealDateAsc(String serviceCode, LocalDate startDate, LocalDate endDate);
    List<MealMenu> findByMealDate(LocalDate mealDate);
}
