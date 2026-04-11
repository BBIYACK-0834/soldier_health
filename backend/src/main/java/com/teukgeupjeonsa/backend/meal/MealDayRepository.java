package com.teukgeupjeonsa.backend.meal;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealDayRepository extends JpaRepository<MealDay, Long> {
    Optional<MealDay> findByUnitAndMealDate(MilitaryUnit unit, LocalDate mealDate);
    List<MealDay> findByUnitAndMealDateBetweenOrderByMealDateAsc(MilitaryUnit unit, LocalDate startDate, LocalDate endDate);
}
