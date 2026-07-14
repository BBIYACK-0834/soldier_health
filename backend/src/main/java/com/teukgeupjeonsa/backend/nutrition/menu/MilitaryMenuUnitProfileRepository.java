package com.teukgeupjeonsa.backend.nutrition.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MilitaryMenuUnitProfileRepository extends JpaRepository<MilitaryMenuUnitProfile, Long> {
    Optional<MilitaryMenuUnitProfile> findFirstByMenuAndUnitCode(MilitaryMenuProfile menu, String unitCode);
}
