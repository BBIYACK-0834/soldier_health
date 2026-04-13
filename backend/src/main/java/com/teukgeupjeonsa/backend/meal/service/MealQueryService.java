package com.teukgeupjeonsa.backend.meal.service;

import com.teukgeupjeonsa.backend.meal.entity.MilitaryMeal;
import com.teukgeupjeonsa.backend.meal.repository.MilitaryMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealQueryService {

    private final MilitaryMealRepository militaryMealRepository;

    @Transactional(readOnly = true)
    public MilitaryMeal getToday(String unitName) {
        return militaryMealRepository.findTopByUnitNameAndMealDateOrderByUpdatedAtDesc(unitName, LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("오늘 식단 데이터가 없습니다. unitName=" + unitName));
    }

    @Transactional(readOnly = true)
    public MilitaryMeal getByDate(String unitName, LocalDate date) {
        return militaryMealRepository.findTopByUnitNameAndMealDateOrderByUpdatedAtDesc(unitName, date)
                .orElseThrow(() -> new IllegalArgumentException("식단 데이터가 없습니다. unitName=" + unitName + ", date=" + date));
    }

    @Transactional(readOnly = true)
    public List<String> getCollectedUnits() {
        return militaryMealRepository.findDistinctUnitNames();
    }
}
