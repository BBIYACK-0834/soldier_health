package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserMealConsumptionRepository extends JpaRepository<UserMealConsumption, Long> {
    List<UserMealConsumption> findByUserAndMealDate(User user, LocalDate mealDate);
    Optional<UserMealConsumption> findByUserAndMealDateAndMealType(User user, LocalDate mealDate, String mealType);
}
