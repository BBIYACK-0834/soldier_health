package com.teukgeupjeonsa.backend.meal.repository;

import com.teukgeupjeonsa.backend.meal.entity.DatasetSource;
import com.teukgeupjeonsa.backend.meal.entity.MilitaryMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MilitaryMealRepository extends JpaRepository<MilitaryMeal, Long> {
    Optional<MilitaryMeal> findBySourceAndUnitNameAndMealDate(DatasetSource source, String unitName, LocalDate mealDate);
    Optional<MilitaryMeal> findTopByUnitNameAndMealDateOrderByUpdatedAtDesc(String unitName, LocalDate mealDate);
    @Query("select distinct m.unitName from MilitaryMeal m order by m.unitName asc")
    List<String> findDistinctUnitNames();
}
