package com.teukgeupjeonsa.backend.unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MilitaryUnitRepository extends JpaRepository<MilitaryUnit, Long> {
    List<MilitaryUnit> findByUnitNameContainingIgnoreCase(String keyword);
    Optional<MilitaryUnit> findByUnitNameIgnoreCase(String unitName);
}
