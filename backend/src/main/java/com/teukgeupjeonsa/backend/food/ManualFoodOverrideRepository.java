package com.teukgeupjeonsa.backend.food;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManualFoodOverrideRepository extends JpaRepository<ManualFoodOverride, Long> {
    Optional<ManualFoodOverride> findFirstByNormalizedMenuName(String normalizedMenuName);
}
