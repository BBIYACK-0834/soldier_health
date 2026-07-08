package com.teukgeupjeonsa.backend.nutrition.matching;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealMenuOccurrenceRepository extends JpaRepository<MealMenuOccurrence, Long> {
    Optional<MealMenuOccurrence> findFirstByNormalizedMenuName(String normalizedMenuName);
    List<MealMenuOccurrence> findByOccurrenceCountGreaterThanEqualOrderByOccurrenceCountDescLastSeenDateDesc(Integer occurrenceCount, Pageable pageable);
}
