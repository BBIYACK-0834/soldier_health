package com.teukgeupjeonsa.backend.meal;

import com.teukgeupjeonsa.backend.unit.UserUnitSetting;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealDayRepository mealDayRepository;
    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;

    @Transactional(readOnly = true)
    public MealDtos.MealDayResponse getToday(Long userId) {
        return getByDate(userId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public MealDtos.MealDayResponse getByDate(Long userId, LocalDate date) {
        UserUnitSetting setting = getPrimaryUnit(userId);
        MealDay mealDay = mealDayRepository.findByUnitAndMealDate(setting.getUnit(), date)
                .orElseThrow(() -> new EntityNotFoundException("해당 날짜 식단이 없습니다."));
        return toResponse(mealDay);
    }

    @Transactional(readOnly = true)
    public List<MealDtos.MealDayResponse> getWeek(Long userId, LocalDate startDate) {
        UserUnitSetting setting = getPrimaryUnit(userId);
        return mealDayRepository.findByUnitAndMealDateBetweenOrderByMealDateAsc(
                setting.getUnit(), startDate, startDate.plusDays(6)
        ).stream().map(this::toResponse).toList();
    }

    private UserUnitSetting getPrimaryUnit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new EntityNotFoundException("먼저 부대를 설정해 주세요."));
    }

    private MealDtos.MealDayResponse toResponse(MealDay mealDay) {
        return MealDtos.MealDayResponse.builder()
                .id(mealDay.getId())
                .mealDate(mealDay.getMealDate())
                .unitName(mealDay.getUnit().getUnitName())
                .breakfastRaw(mealDay.getBreakfastRaw())
                .lunchRaw(mealDay.getLunchRaw())
                .dinnerRaw(mealDay.getDinnerRaw())
                .breakfastKcal(mealDay.getBreakfastKcal())
                .lunchKcal(mealDay.getLunchKcal())
                .dinnerKcal(mealDay.getDinnerKcal())
                .build();
    }
}
