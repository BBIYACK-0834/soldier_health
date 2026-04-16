package com.teukgeupjeonsa.backend.collector.repository;

import com.teukgeupjeonsa.backend.collector.entity.UnitApiSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitApiSourceRepository extends JpaRepository<UnitApiSource, Long> {
    Optional<UnitApiSource> findByServiceName(String serviceName);
    Optional<UnitApiSource> findTopByUnitNameIgnoreCaseAndActiveTrue(String unitName);
    List<UnitApiSource> findByActiveTrueOrderByUnitNameAsc();
}
