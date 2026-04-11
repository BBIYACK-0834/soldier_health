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
import java.util.Optional;

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
        Optional<UserUnitSetting> settingOptional = getPrimaryUnit(userId);
        if (settingOptional.isEmpty()) {
            return emptyMealResponse(date);
        }

        return mealDayRepository.findByUnitAndMealDate(settingOptional.get().getUnit(), date)
                .map(this::toResponse)
                .orElseGet(() -> emptyMealResponse(date));
    }

    @Transactional(readOnly = true)
    public List<MealDtos.MealDayResponse> getWeek(Long userId, LocalDate startDate) {
        Optional<UserUnitSetting> settingOptional = getPrimaryUnit(userId);
        if (settingOptional.isEmpty()) {
            return List.of();
        }

        return mealDayRepository.findByUnitAndMealDateBetweenOrderByMealDateAsc(
                settingOptional.get().getUnit(), startDate, startDate.plusDays(6)
        ).stream().map(this::toResponse).toList();
    }

    private Optional<UserUnitSetting> getPrimaryUnit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user);
    }

    private MealDtos.MealDayResponse emptyMealResponse(LocalDate date) {
        return MealDtos.MealDayResponse.builder()
                .mealDate(date)
                .breakfastRaw(null)
                .lunchRaw(null)
                .dinnerRaw(null)
                .breakfastKcal(0)
                .lunchKcal(0)
                .dinnerKcal(0)
                .build();
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
