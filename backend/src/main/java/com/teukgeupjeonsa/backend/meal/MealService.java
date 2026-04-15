package com.teukgeupjeonsa.backend.meal;

import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
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

    private final MealMenuRepository mealMenuRepository;
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

        String serviceCode = settingOptional.get().getUnit().getDataSourceKey();
        if (serviceCode == null || serviceCode.isBlank()) {
            return emptyMealResponse(date);
        }

        return mealMenuRepository.findTopByServiceCodeAndMealDateOrderByUpdatedAtDesc(serviceCode, date)
                .map(mealMenu -> toResponse(settingOptional.get(), mealMenu))
                .orElseGet(() -> emptyMealResponse(date));
    }

    @Transactional(readOnly = true)
    public List<MealDtos.MealDayResponse> getWeek(Long userId, LocalDate startDate) {
        Optional<UserUnitSetting> settingOptional = getPrimaryUnit(userId);
        if (settingOptional.isEmpty()) {
            return List.of();
        }

        String serviceCode = settingOptional.get().getUnit().getDataSourceKey();
        if (serviceCode == null || serviceCode.isBlank()) {
            return List.of();
        }

        return mealMenuRepository.findByServiceCodeAndMealDateBetweenOrderByMealDateAsc(
                        serviceCode, startDate, startDate.plusDays(6)
                ).stream()
                .map(mealMenu -> toResponse(settingOptional.get(), mealMenu))
                .toList();
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
                .breakfastKcal(null)
                .lunchKcal(null)
                .dinnerKcal(null)
                .totalKcal(null)
                .build();
    }

    private MealDtos.MealDayResponse toResponse(UserUnitSetting setting, MealMenu mealMenu) {
        return MealDtos.MealDayResponse.builder()
                .id(mealMenu.getId())
                .mealDate(mealMenu.getMealDate())
                .unitName(setting.getUnit().getUnitName())
                .sourceName(mealMenu.getSourceName())
                .serviceCode(mealMenu.getServiceCode())
                .breakfastRaw(mealMenu.getBreakfast())
                .lunchRaw(mealMenu.getLunch())
                .dinnerRaw(mealMenu.getDinner())
                .breakfastKcal(mealMenu.getBreakfastKcal())
                .lunchKcal(mealMenu.getLunchKcal())
                .dinnerKcal(mealMenu.getDinnerKcal())
                .totalKcal(mealMenu.getTotalKcal())
                .build();
    }
}
