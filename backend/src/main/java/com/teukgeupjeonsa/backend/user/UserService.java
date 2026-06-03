package com.teukgeupjeonsa.backend.user;

import com.teukgeupjeonsa.backend.unit.UnitResponse;
import com.teukgeupjeonsa.backend.unit.UserUnitSetting;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = getUser(userId);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setHeightCm(request.getHeightCm());
        user.setWeightKg(request.getWeightKg());
        user.setRank(normalizeRank(request.getRank()));
        user.setDischargeDate(request.getDischargeDate());
        user.setPromotionDate(request.getPromotionDate());
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateGoals(Long userId, UpdateGoalsRequest request) {
        User user = getUser(userId);
        user.setGoalType(request.getGoalType());
        user.setWorkoutLevel(request.getWorkoutLevel());
        user.setBranchType(request.getBranchType());
        user.setWorkoutDaysPerWeek(request.getWorkoutDaysPerWeek());
        user.setPreferredWorkoutMinutes(request.getPreferredWorkoutMinutes());
        return toResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private String normalizeRank(String rank) {
        if (rank == null || rank.isBlank()) {
            return null;
        }
        return rank.trim();
    }

    private UserProfileResponse toResponse(User user) {
        UnitResponse unit = userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .map(UserUnitSetting::getUnit)
                .map(selectedUnit -> UnitResponse.builder()
                        .id(selectedUnit.getId())
                        .unitCode(selectedUnit.getUnitCode())
                        .unitName(selectedUnit.getUnitName())
                        .branchType(selectedUnit.getBranchType())
                        .regionName(selectedUnit.getRegionName())
                        .dataSourceKey(selectedUnit.getDataSourceKey())
                        .build())
                .orElse(null);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .goalType(user.getGoalType())
                .workoutLevel(user.getWorkoutLevel())
                .workoutDaysPerWeek(user.getWorkoutDaysPerWeek())
                .preferredWorkoutMinutes(user.getPreferredWorkoutMinutes())
                .branchType(user.getBranchType())
                .rank(user.getRank())
                .dischargeDate(user.getDischargeDate())
                .promotionDate(user.getPromotionDate())
                .unitId(unit != null ? unit.getId() : null)
                .unitName(unit != null ? unit.getUnitName() : null)
                .unit(unit)
                .build();
    }
}
