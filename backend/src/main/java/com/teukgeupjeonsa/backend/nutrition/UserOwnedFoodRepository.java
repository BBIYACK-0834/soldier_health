package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserOwnedFoodRepository extends JpaRepository<UserOwnedFood, Long> {
    List<UserOwnedFood> findByUser(User user);
}
