package com.teukgeupjeonsa.backend.unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilitaryUnitRepository extends JpaRepository<MilitaryUnit, Long> {
    List<MilitaryUnit> findByUnitNameContainingIgnoreCase(String keyword);
}
