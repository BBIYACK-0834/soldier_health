package com.teukgeupjeonsa.backend.nutrition.matching;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMatchCandidateRepository extends JpaRepository<FoodMatchCandidate, Long> {
    List<FoodMatchCandidate> findByStatusAndOccurrenceCountGreaterThanEqualAndScoreGreaterThanEqualOrderByOccurrenceCountDescScoreDescUpdatedAtDesc(
            FoodMatchEnums.CandidateStatus status, Integer occurrenceCount, Double score, Pageable pageable);
}
