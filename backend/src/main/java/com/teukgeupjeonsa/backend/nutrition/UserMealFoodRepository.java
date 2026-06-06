package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserMealFoodRepository extends JpaRepository<UserMealFood, Long> {
    List<UserMealFood> findByUserAndMealDate(User user, LocalDate mealDate);
    List<UserMealFood> findByUserAndMealDateAndMealType(User user, LocalDate mealDate, String mealType);
}
