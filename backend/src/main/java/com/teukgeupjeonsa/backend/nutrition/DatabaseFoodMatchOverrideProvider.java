package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import com.teukgeupjeonsa.backend.food.ManualFoodOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
public class DatabaseFoodMatchOverrideProvider implements FoodMatchOverrideProvider {

    private final ManualFoodOverrideRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ManualFoodOverride> findOverride(String normalizedMenuName) {
        return repository.findFirstByNormalizedMenuName(normalizedMenuName);
    }
}
