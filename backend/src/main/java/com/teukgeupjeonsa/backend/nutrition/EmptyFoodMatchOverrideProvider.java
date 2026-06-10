package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmptyFoodMatchOverrideProvider implements FoodMatchOverrideProvider {
    @Override
    public Optional<Food> findOverride(String normalizedMenuName) {
        // TODO: food_match_override 테이블이 추가되면 이 구현체를 Repository 기반으로 교체한다.
        return Optional.empty();
    }
}
