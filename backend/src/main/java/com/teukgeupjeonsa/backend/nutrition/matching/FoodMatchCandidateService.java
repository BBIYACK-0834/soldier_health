package com.teukgeupjeonsa.backend.nutrition.matching;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import com.teukgeupjeonsa.backend.food.ManualFoodOverrideRepository;
import com.teukgeupjeonsa.backend.nutrition.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodMatchCandidateService {
    public static final double AUTO_APPROVE_SCORE = 0.93;
    public static final double REVIEW_SCORE = 0.78;
    public static final int REVIEW_MIN_OCCURRENCE = 3;

    private final MealMenuOccurrenceRepository occurrenceRepository;
    private final FoodMatchCandidateRepository candidateRepository;
    private final ManualFoodOverrideRepository manualFoodOverrideRepository;
    private final FoodMatcher foodMatcher;
    private final CompositeFoodEstimator compositeFoodEstimator;
    private final MealNutritionService mealNutritionService;

    @Transactional
    public RebuildResult rebuildCandidates() {
        candidateRepository.deleteAllInBatch();
        List<FoodMatchCandidate> candidates = occurrenceRepository.findAll().stream()
                .map(this::buildCandidate)
                .filter(candidate -> candidate.getScore() >= REVIEW_SCORE || candidate.getFood() != null)
                .toList();
        List<FoodMatchCandidate> saved = candidateRepository.saveAll(candidates);
        int autoApproved = 0;
        int needsReview = 0;
        for (FoodMatchCandidate candidate : saved) {
            if (candidate.getStatus() == FoodMatchEnums.CandidateStatus.AUTO_APPROVED && candidate.getFood() != null) {
                autoApproved++;
                saveOverride(candidate, "auto approved");
            } else if (candidate.getStatus() == FoodMatchEnums.CandidateStatus.NEEDS_REVIEW) {
                needsReview++;
            }
        }
        if (autoApproved > 0) mealNutritionService.clearAnalysisCache();
        return RebuildResult.builder().processedOccurrences(saved.size()).autoApproved(autoApproved).needsReview(needsReview).build();
    }

    @Transactional(readOnly = true)
    public List<FoodMatchCandidate> reviewQueue(int limit) {
        return candidateRepository.findByStatusAndOccurrenceCountGreaterThanEqualAndScoreGreaterThanEqualOrderByOccurrenceCountDescScoreDescUpdatedAtDesc(
                FoodMatchEnums.CandidateStatus.NEEDS_REVIEW, REVIEW_MIN_OCCURRENCE, REVIEW_SCORE, PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    @Transactional
    public FoodMatchCandidate approve(Long candidateId) {
        FoodMatchCandidate candidate = candidateRepository.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("후보를 찾을 수 없습니다."));
        if (candidate.getFood() == null) throw new IllegalArgumentException("승인할 식품 후보가 없습니다.");
        candidate.setStatus(FoodMatchEnums.CandidateStatus.APPROVED);
        saveOverride(candidate, "review approved");
        mealNutritionService.clearAnalysisCache();
        return candidate;
    }

    @Transactional
    public FoodMatchCandidate reject(Long candidateId) {
        FoodMatchCandidate candidate = candidateRepository.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("후보를 찾을 수 없습니다."));
        candidate.setStatus(FoodMatchEnums.CandidateStatus.REJECTED);
        return candidate;
    }

    FoodMatchCandidate buildCandidate(MealMenuOccurrence occurrence) {
        FoodMatchResult match = foodMatcher.match(occurrence.getRawMenuName());
        if (match.isMatched()) {
            double score = scoreFor(match);
            FoodMatchEnums.CandidateStatus status = classifyStatus(occurrence.getNormalizedMenuName(), score, occurrence.getOccurrenceCount());
            return FoodMatchCandidate.builder()
                    .rawMenuName(occurrence.getRawMenuName())
                    .normalizedMenuName(occurrence.getNormalizedMenuName())
                    .food(match.getMatchedFood())
                    .matchedFoodName(match.getMatchedFoodName())
                    .matchType(match.getMatchType())
                    .score(score)
                    .confidence(confidenceFor(score))
                    .status(status)
                    .occurrenceCount(occurrence.getOccurrenceCount())
                    .defaultServingGram(match.getDefaultServingGram())
                    .reason("FoodMatcher " + match.getMatchType() + " score=" + score)
                    .build();
        }
        return compositeFoodEstimator.estimate(occurrence.getRawMenuName())
                .map(estimate -> FoodMatchCandidate.builder()
                        .rawMenuName(occurrence.getRawMenuName())
                        .normalizedMenuName(occurrence.getNormalizedMenuName())
                        .matchedFoodName(estimate.getMatchedDisplayName())
                        .matchType("COMPOSITE")
                        .score(0.72)
                        .confidence(FoodMatchEnums.CandidateConfidence.LOW)
                        .status(classifyStatus(occurrence.getNormalizedMenuName(), 0.72, occurrence.getOccurrenceCount()))
                        .occurrenceCount(occurrence.getOccurrenceCount())
                        .defaultServingGram(estimate.getServingGram())
                        .reason("CompositeFoodEstimator ingredients=" + estimate.getIngredients().size())
                        .build())
                .orElseGet(() -> FoodMatchCandidate.builder()
                        .rawMenuName(occurrence.getRawMenuName())
                        .normalizedMenuName(occurrence.getNormalizedMenuName())
                        .matchType("NO_MATCH")
                        .score(0.0)
                        .confidence(FoodMatchEnums.CandidateConfidence.LOW)
                        .status(FoodMatchEnums.CandidateStatus.REJECTED)
                        .occurrenceCount(occurrence.getOccurrenceCount())
                        .reason("No candidate")
                        .build());
    }

    FoodMatchEnums.CandidateStatus classifyStatus(String normalizedName, double score, int occurrenceCount) {
        if (score >= AUTO_APPROVE_SCORE && isAutoApprovalSafe(normalizedName)) return FoodMatchEnums.CandidateStatus.AUTO_APPROVED;
        if (score >= REVIEW_SCORE && occurrenceCount >= REVIEW_MIN_OCCURRENCE) return FoodMatchEnums.CandidateStatus.NEEDS_REVIEW;
        return FoodMatchEnums.CandidateStatus.REJECTED;
    }

    private double scoreFor(FoodMatchResult match) {
        String type = match.getMatchType();
        if ("OVERRIDE_EXACT".equals(type)) return 1.0;
        if ("ALIAS_EXACT".equals(type)) return 0.99;
        if ("FOOD_EXACT".equals(type)) return 0.98;
        if ("CONTAINS".equals(type)) return clamp(match.getScore(), 0.80, 0.94);
        if ("TOKEN_CONTAINS".equals(type)) return clamp(match.getScore(), 0.70, 0.90);
        if ("SIMILARITY".equals(type)) return clamp(match.getScore(), 0.65, 0.85);
        return match.getScore() == null ? 0.0 : match.getScore();
    }

    private boolean isAutoApprovalSafe(String normalizedName) {
        if (normalizedName == null) return false;
        if (List.of("밥", "쌀밥", "잡곡밥", "김치", "배추김치").contains(normalizedName)) return true;
        return normalizedName.length() > 2 && !List.of("국", "탕", "전", "차", "빵").contains(normalizedName);
    }

    private FoodMatchEnums.CandidateConfidence confidenceFor(double score) {
        if (score >= AUTO_APPROVE_SCORE) return FoodMatchEnums.CandidateConfidence.HIGH;
        if (score >= REVIEW_SCORE) return FoodMatchEnums.CandidateConfidence.MEDIUM;
        return FoodMatchEnums.CandidateConfidence.LOW;
    }

    private void saveOverride(FoodMatchCandidate candidate, String prefix) {
        String note = prefix + " / score=" + candidate.getScore() + " / matchType=" + candidate.getMatchType();
        ManualFoodOverride override = manualFoodOverrideRepository.findFirstByNormalizedMenuName(candidate.getNormalizedMenuName()).orElseGet(ManualFoodOverride::new);
        override.setRawMenuName(candidate.getRawMenuName());
        override.setNormalizedMenuName(candidate.getNormalizedMenuName());
        override.setFood(candidate.getFood());
        override.setConfidence(MatchConfidence.HIGH);
        override.setDefaultServingGram(candidate.getDefaultServingGram());
        override.setNote(note);
        manualFoodOverrideRepository.save(override);
    }

    private double clamp(Double value, double min, double max) {
        if (value == null) return min;
        return Math.max(min, Math.min(max, value));
    }

    @Getter @Builder
    public static class RebuildResult {
        private int processedOccurrences;
        private int autoApproved;
        private int needsReview;
    }
}
