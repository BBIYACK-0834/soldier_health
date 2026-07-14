package com.teukgeupjeonsa.backend.nutrition.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MilitaryMenuDailyProfileRepository extends JpaRepository<MilitaryMenuDailyProfile, Long> {
    Optional<MilitaryMenuDailyProfile> findFirstByUnitCodeAndMealDateAndMealTypeAndSearchName(
            String unitCode, LocalDate mealDate, String mealType, String searchName);
}
