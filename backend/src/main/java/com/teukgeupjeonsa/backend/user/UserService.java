package com.teukgeupjeonsa.backend.user;

import com.teukgeupjeonsa.backend.unit.UnitResponse;
import com.teukgeupjeonsa.backend.unit.UserUnitSetting;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int ARMY_SERVICE_MONTHS = 18;
    private static final int PRIVATE_FIRST_CLASS_MONTH = 2;
    private static final int CORPORAL_MONTH = 8;
    private static final int SERGEANT_MONTH = 14;
    private static final long MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;

    @Value("${app.upload.profile-images-dir:uploads/profile-images}")
    private String profileImagesDir;

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
    public UserProfileResponse uploadProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 프로필 이미지를 선택해주세요.");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_BYTES) {
            throw new IllegalArgumentException("프로필 이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        String extension = resolveImageExtension(contentType);
        String filename = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(profileImagesDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
            file.transferTo(uploadPath.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지를 저장하지 못했습니다.", e);
        }

        User user = getUser(userId);
        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/profile-images/")
                .path(filename)
                .toUriString();
        user.setProfileImageUrl(imageUrl);

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

    private String resolveImageExtension(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".jpg";
        };
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
                .rank(serviceInfo.rank())
                .dischargeDate(serviceInfo.dischargeDate())
                .promotionDate(null)
                .enlistmentDate(user.getEnlistmentDate())
                .nextPromotionDate(serviceInfo.nextPromotionDate())
                .daysUntilDischarge(serviceInfo.daysUntilDischarge())
                .serviceProgressPercent(serviceInfo.serviceProgressPercent())
                .unitId(unit != null ? unit.getId() : null)
                .unitName(unit != null ? unit.getUnitName() : null)
                .unit(unit)
                .build();
    }

    private MilitaryServiceInfo calculateMilitaryServiceInfo(LocalDate enlistmentDate) {
        if (enlistmentDate == null) {
            return new MilitaryServiceInfo(null, null, null, null, null);
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
