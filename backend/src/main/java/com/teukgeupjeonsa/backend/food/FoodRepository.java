package com.teukgeupjeonsa.backend.food;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByNameContainingOrSearchNameContaining(String name, String searchName);
}
