package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.collector.service.MealCollectorServiceCodeResolver;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final MilitaryUnitRepository militaryUnitRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;
    private final UserRepository userRepository;
    private final MealCollectorServiceCodeResolver serviceCodeResolver;

    @Transactional(readOnly = true)
    public List<UnitResponse> getUnits(String keyword) {
        List<String> fixedServiceCodes = serviceCodeResolver.resolveFixedServiceCodes();
        List<MilitaryUnit> units = (keyword == null || keyword.isBlank())
                ? militaryUnitRepository.findByDataSourceKeyIn(fixedServiceCodes)
                : militaryUnitRepository.findByDataSourceKeyInAndUnitNameContainingIgnoreCase(fixedServiceCodes, keyword);

        return units.stream()
                .sorted(Comparator.comparing(MilitaryUnit::getUnitName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UnitResponse setMyUnit(Long userId, Long unitId) {
        List<String> fixedServiceCodes = serviceCodeResolver.resolveFixedServiceCodes();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        MilitaryUnit unit = militaryUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("부대를 찾을 수 없습니다."));
        if (unit.getDataSourceKey() == null || !fixedServiceCodes.contains(unit.getDataSourceKey().trim())) {
            throw new IllegalArgumentException("지원하지 않는 부대입니다. 고정 서비스 목록의 부대를 선택해주세요.");
        }

        userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(setting -> setting.setIsPrimary(false));

        UserUnitSetting setting = userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .orElse(UserUnitSetting.builder().user(user).unit(unit).isPrimary(true).build());
        setting.setUnit(unit);
        setting.setIsPrimary(true);
        userUnitSettingRepository.save(setting);

        return toResponse(unit);
    }

    @Transactional(readOnly = true)
    public UnitResponse getMyUnit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .map(UserUnitSetting::getUnit)
                .map(this::toResponse)
                .orElse(null);
    }

    private UnitResponse toResponse(MilitaryUnit unit) {
        return UnitResponse.builder()
                .id(unit.getId())
                .unitCode(unit.getUnitCode())
                .unitName(unit.getUnitName())
                .branchType(unit.getBranchType())
                .regionName(unit.getRegionName())
                .build();
    }
}
