package com.teukgeupjeonsa.backend.meal.repository;

import com.teukgeupjeonsa.backend.meal.entity.DatasetSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetSourceRepository extends JpaRepository<DatasetSource, Long> {
    Optional<DatasetSource> findBySourceUrl(String sourceUrl);
}
