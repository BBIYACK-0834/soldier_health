package com.teukgeupjeonsa.backend.user;

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

    private UserProfileResponse toResponse(User user) {
        MilitaryServiceInfo serviceInfo = calculateMilitaryServiceInfo(user.getEnlistmentDate());

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
                .enlistmentDate(user.getEnlistmentDate())
                .dischargeDate(serviceInfo.dischargeDate())
                .rank(serviceInfo.rank())
                .nextPromotionDate(serviceInfo.nextPromotionDate())
                .daysUntilDischarge(serviceInfo.daysUntilDischarge())
                .serviceProgressPercent(serviceInfo.serviceProgressPercent())
                .build();
    }

    private String normalizeProfileImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private MilitaryServiceInfo calculateMilitaryServiceInfo(LocalDate enlistmentDate) {
        if (enlistmentDate == null) {
            return new MilitaryServiceInfo(null, "이병", null, null, null);
        }

        LocalDate today = LocalDate.now();
        LocalDate dischargeDate = enlistmentDate.plusMonths(ARMY_SERVICE_MONTHS).minusDays(1);
        String rank = calculateRank(enlistmentDate, today);
        LocalDate nextPromotionDate = calculateNextPromotionDate(enlistmentDate, today);
        long daysUntilDischarge = Math.max(0, ChronoUnit.DAYS.between(today, dischargeDate));
        long totalServiceDays = Math.max(1, ChronoUnit.DAYS.between(enlistmentDate, dischargeDate) + 1);
        long servedDays = Math.max(0, ChronoUnit.DAYS.between(enlistmentDate, today));
        double progressPercent = Math.min(100.0, Math.max(0.0, (servedDays * 100.0) / totalServiceDays));

        return new MilitaryServiceInfo(dischargeDate, rank, nextPromotionDate, daysUntilDischarge, progressPercent);
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
