package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnMissingBean(FoodMatchOverrideProvider.class)
public class EmptyFoodMatchOverrideProvider implements FoodMatchOverrideProvider {
    @Override
    public Optional<ManualFoodOverride> findOverride(String normalizedMenuName) {
        return Optional.empty();
    }
}
