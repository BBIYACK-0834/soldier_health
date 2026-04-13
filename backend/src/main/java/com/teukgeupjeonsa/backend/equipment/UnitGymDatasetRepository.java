package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitGymDatasetRepository extends JpaRepository<UnitGymDataset, Long> {
    List<UnitGymDataset> findByUnitOrderByIdDesc(MilitaryUnit unit);
}
