package com.teukgeupjeonsa.backend.nutrition.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MilitaryMenuProfileRepository extends JpaRepository<MilitaryMenuProfile, Long> {
    Optional<MilitaryMenuProfile> findFirstBySearchName(String searchName);
}
