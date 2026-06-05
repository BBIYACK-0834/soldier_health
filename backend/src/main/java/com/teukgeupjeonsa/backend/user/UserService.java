package com.teukgeupjeonsa.backend.user;

import com.teukgeupjeonsa.backend.unit.UnitResponse;
import com.teukgeupjeonsa.backend.unit.UserUnitSetting;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int ARMY_SERVICE_MONTHS = 18;
    private static final int PRIVATE_FIRST_CLASS_MONTH = 3;
    private static final int CORPORAL_MONTH = 9;
    private static final int SERGEANT_MONTH = 12;

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
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(normalizeProfileImageUrl(request.getProfileImageUrl()));
        }
        if (request.getHeightCm() != null) {
            user.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            user.setWeightKg(request.getWeightKg());
        }
        if (request.getRank() != null) {
            user.setRank(normalizeRank(request.getRank()));
        }
        if (request.getDischargeDate() != null) {
            user.setDischargeDate(request.getDischargeDate());
        }
        if (request.getPromotionDate() != null) {
            user.setPromotionDate(request.getPromotionDate());
        }
        if (request.getEnlistmentDate() != null) {
            user.setEnlistmentDate(request.getEnlistmentDate());
        }
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

    private String normalizeProfileImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
        MilitaryServiceInfo serviceInfo = calculateMilitaryServiceInfo(user.getEnlistmentDate(), user.getDischargeDate(), user.getPromotionDate());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .goalType(user.getGoalType())
                .workoutLevel(user.getWorkoutLevel())
                .workoutDaysPerWeek(user.getWorkoutDaysPerWeek())
                .preferredWorkoutMinutes(user.getPreferredWorkoutMinutes())
                .branchType(user.getBranchType())
                .rank(user.getRank() != null ? user.getRank() : serviceInfo.rank())
                .dischargeDate(user.getDischargeDate() != null ? user.getDischargeDate() : serviceInfo.dischargeDate())
                .promotionDate(user.getPromotionDate())
                .enlistmentDate(user.getEnlistmentDate())
                .nextPromotionDate(serviceInfo.nextPromotionDate())
                .daysUntilDischarge(serviceInfo.daysUntilDischarge())
                .serviceProgressPercent(serviceInfo.serviceProgressPercent())
                .unitId(unit != null ? unit.getId() : null)
                .unitName(unit != null ? unit.getUnitName() : null)
                .unit(unit)
                .build();
    }

    private MilitaryServiceInfo calculateMilitaryServiceInfo(LocalDate enlistmentDate, LocalDate manualDischargeDate, LocalDate manualPromotionDate) {
        if (enlistmentDate == null) {
            return new MilitaryServiceInfo(manualDischargeDate, null, manualPromotionDate, calculateDaysUntil(manualDischargeDate), null);
        }

        LocalDate today = LocalDate.now();
        LocalDate dischargeDate = manualDischargeDate != null ? manualDischargeDate : enlistmentDate.plusMonths(ARMY_SERVICE_MONTHS).minusDays(1);
        String rank = calculateRank(enlistmentDate, today);
        LocalDate nextPromotionDate = manualPromotionDate != null ? manualPromotionDate : calculateNextPromotionDate(enlistmentDate, today);
        long daysUntilDischarge = Math.max(0, ChronoUnit.DAYS.between(today, dischargeDate));
        long totalServiceDays = Math.max(1, ChronoUnit.DAYS.between(enlistmentDate, dischargeDate) + 1);
        long servedDays = Math.max(0, ChronoUnit.DAYS.between(enlistmentDate, today));
        double progressPercent = Math.min(100.0, Math.max(0.0, (servedDays * 100.0) / totalServiceDays));

        return new MilitaryServiceInfo(dischargeDate, rank, nextPromotionDate, daysUntilDischarge, progressPercent);
    }

    private Long calculateDaysUntil(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), date));
    }

    private String calculateRank(LocalDate enlistmentDate, LocalDate baseDate) {
        if (!baseDate.isBefore(enlistmentDate.plusMonths(SERGEANT_MONTH))) {
            return "병장";
        }
        if (!baseDate.isBefore(enlistmentDate.plusMonths(CORPORAL_MONTH))) {
            return "상병";
        }
        if (!baseDate.isBefore(enlistmentDate.plusMonths(PRIVATE_FIRST_CLASS_MONTH))) {
            return "일병";
        }
        return "이병";
    }

    private LocalDate calculateNextPromotionDate(LocalDate enlistmentDate, LocalDate baseDate) {
        LocalDate privateFirstClassDate = enlistmentDate.plusMonths(PRIVATE_FIRST_CLASS_MONTH);
        if (baseDate.isBefore(privateFirstClassDate)) {
            return privateFirstClassDate;
        }

        LocalDate corporalDate = enlistmentDate.plusMonths(CORPORAL_MONTH);
        if (baseDate.isBefore(corporalDate)) {
            return corporalDate;
        }

        LocalDate sergeantDate = enlistmentDate.plusMonths(SERGEANT_MONTH);
        if (baseDate.isBefore(sergeantDate)) {
            return sergeantDate;
        }

        return null;
    }

    private record MilitaryServiceInfo(
            LocalDate dischargeDate,
            String rank,
            LocalDate nextPromotionDate,
            Long daysUntilDischarge,
            Double serviceProgressPercent
    ) {
    }
}
