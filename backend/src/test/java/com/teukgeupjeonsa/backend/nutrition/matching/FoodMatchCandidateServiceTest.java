package com.teukgeupjeonsa.backend.nutrition.matching;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import com.teukgeupjeonsa.backend.food.ManualFoodOverrideRepository;
import com.teukgeupjeonsa.backend.nutrition.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodMatchCandidateServiceTest {
    @Mock MealMenuOccurrenceRepository occurrenceRepository;
    @Mock FoodMatchCandidateRepository candidateRepository;
    @Mock ManualFoodOverrideRepository manualFoodOverrideRepository;
    @Mock FoodMatcher foodMatcher;
    @Mock CompositeFoodEstimator compositeFoodEstimator;
    @Mock MealNutritionService mealNutritionService;

    @Test
    void classifiesHighScoreSafeMenuAsAutoApprovedButShortRiskyAsReview() {
        FoodMatchCandidateService service = service();
        assertThat(service.classifyStatus("닭순살카레조림", 0.98, 10)).isEqualTo(FoodMatchEnums.CandidateStatus.AUTO_APPROVED);
        assertThat(service.classifyStatus("국", 0.98, 10)).isEqualTo(FoodMatchEnums.CandidateStatus.NEEDS_REVIEW);
        assertThat(service.classifyStatus("돈육김치두루치기", 0.86, 5)).isEqualTo(FoodMatchEnums.CandidateStatus.NEEDS_REVIEW);
        assertThat(service.classifyStatus("돈육김치두루치기", 0.86, 1)).isEqualTo(FoodMatchEnums.CandidateStatus.REJECTED);
    }

    @Test
    void buildCandidateUsesMatcherScorePolicy() {
        Food food = Food.builder().id(1L).name("닭고기 카레").category("볶음류").servingUnit("100g").build();
        when(foodMatcher.match("닭순살카레조림")).thenReturn(FoodMatchResult.builder()
                .originalMenuName("닭순살카레조림")
                .normalizedMenuName("닭순살카레조림")
                .matched(true)
                .matchedFood(food)
                .matchedFoodId(1L)
                .matchedFoodName("닭고기 카레")
                .matchType("ALIAS_EXACT")
                .score(1.0)
                .defaultServingGram(150.0)
                .confidence(MatchConfidence.HIGH)
                .build());
        FoodMatchCandidateService service = service();

        FoodMatchCandidate candidate = service.buildCandidate(MealMenuOccurrence.builder()
                .rawMenuName("닭순살카레조림")
                .normalizedMenuName("닭순살카레조림")
                .occurrenceCount(42)
                .build());

        assertThat(candidate.getScore()).isEqualTo(0.99);
        assertThat(candidate.getStatus()).isEqualTo(FoodMatchEnums.CandidateStatus.AUTO_APPROVED);
        assertThat(candidate.getDefaultServingGram()).isEqualTo(150.0);
    }

    @Test
    void approveStoresManualOverride() {
        Food food = Food.builder().id(7L).name("돼지고기 김치볶음").build();
        FoodMatchCandidate candidate = FoodMatchCandidate.builder()
                .id(9L)
                .rawMenuName("돈육김치두루치기")
                .normalizedMenuName("돈육김치두루치기")
                .food(food)
                .matchedFoodName(food.getName())
                .matchType("TOKEN_CONTAINS")
                .score(0.86)
                .status(FoodMatchEnums.CandidateStatus.NEEDS_REVIEW)
                .occurrenceCount(38)
                .defaultServingGram(160.0)
                .build();
        when(candidateRepository.findById(9L)).thenReturn(Optional.of(candidate));
        when(manualFoodOverrideRepository.findFirstByNormalizedMenuName("돈육김치두루치기")).thenReturn(Optional.empty());
        FoodMatchCandidateService service = service();

        FoodMatchCandidate approved = service.approve(9L);

        assertThat(approved.getStatus()).isEqualTo(FoodMatchEnums.CandidateStatus.APPROVED);
        ArgumentCaptor<ManualFoodOverride> captor = ArgumentCaptor.forClass(ManualFoodOverride.class);
        verify(manualFoodOverrideRepository).save(captor.capture());
        verify(mealNutritionService).clearAnalysisCache();
        assertThat(captor.getValue().getRawMenuName()).isEqualTo("돈육김치두루치기");
        assertThat(captor.getValue().getFood()).isEqualTo(food);
        assertThat(captor.getValue().getConfidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(captor.getValue().getNote()).contains("review approved");
    }

    private FoodMatchCandidateService service() {
        return new FoodMatchCandidateService(occurrenceRepository, candidateRepository, manualFoodOverrideRepository, foodMatcher, compositeFoodEstimator, mealNutritionService);
    }
}
