package com.teukgeupjeonsa.backend.nutrition.matching;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/foods/matching")
public class FoodMatchingAdminController {
    private final MealMenuOccurrenceService occurrenceService;
    private final FoodMatchCandidateService candidateService;

    @PostMapping("/occurrences/rebuild")
    public ApiResponse<MealMenuOccurrenceService.RebuildResult> rebuildOccurrences() {
        return ApiResponse.ok(occurrenceService.rebuildOccurrences());
    }

    @PostMapping("/candidates/rebuild")
    public ApiResponse<FoodMatchCandidateService.RebuildResult> rebuildCandidates() {
        return ApiResponse.ok(candidateService.rebuildCandidates());
    }

    @GetMapping("/review")
    public ApiResponse<List<CandidateResponse>> review(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(candidateService.reviewQueue(limit).stream().map(CandidateResponse::from).toList());
    }

    @PostMapping("/candidates/{id}/approve")
    public ApiResponse<CandidateResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(CandidateResponse.from(candidateService.approve(id)));
    }

    @PostMapping("/candidates/{id}/reject")
    public ApiResponse<CandidateResponse> reject(@PathVariable Long id) {
        return ApiResponse.ok(CandidateResponse.from(candidateService.reject(id)));
    }

    @Getter @Builder
    public static class CandidateResponse {
        private Long id;
        private String rawMenuName;
        private String normalizedMenuName;
        private Long foodId;
        private String matchedFoodName;
        private String matchType;
        private Double score;
        private FoodMatchEnums.CandidateConfidence confidence;
        private FoodMatchEnums.CandidateStatus status;
        private Integer occurrenceCount;
        private Double defaultServingGram;
        private String reason;

        static CandidateResponse from(FoodMatchCandidate candidate) {
            return CandidateResponse.builder()
                    .id(candidate.getId())
                    .rawMenuName(candidate.getRawMenuName())
                    .normalizedMenuName(candidate.getNormalizedMenuName())
                    .foodId(candidate.getFood() == null ? null : candidate.getFood().getId())
                    .matchedFoodName(candidate.getMatchedFoodName())
                    .matchType(candidate.getMatchType())
                    .score(candidate.getScore())
                    .confidence(candidate.getConfidence())
                    .status(candidate.getStatus())
                    .occurrenceCount(candidate.getOccurrenceCount())
                    .defaultServingGram(candidate.getDefaultServingGram())
                    .reason(candidate.getReason())
                    .build();
        }
    }
}
