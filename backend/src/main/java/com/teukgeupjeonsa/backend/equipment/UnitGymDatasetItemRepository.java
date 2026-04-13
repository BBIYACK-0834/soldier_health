package com.teukgeupjeonsa.backend.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitGymDatasetItemRepository extends JpaRepository<UnitGymDatasetItem, Long> {
    List<UnitGymDatasetItem> findByDataset(UnitGymDataset dataset);
    void deleteByDataset(UnitGymDataset dataset);
}
