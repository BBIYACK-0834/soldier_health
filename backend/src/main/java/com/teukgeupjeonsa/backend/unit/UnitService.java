package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final MilitaryUnitRepository militaryUnitRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UnitResponse> getUnits(String keyword) {
        List<MilitaryUnit> units = (keyword == null || keyword.isBlank())
                ? militaryUnitRepository.findAll()
                : militaryUnitRepository.findByUnitNameContainingIgnoreCase(keyword);

        return units.stream().map(this::toResponse).toList();
    }

    @Transactional
    public UnitResponse setMyUnit(Long userId, Long unitId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        MilitaryUnit unit = militaryUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("부대를 찾을 수 없습니다."));

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
