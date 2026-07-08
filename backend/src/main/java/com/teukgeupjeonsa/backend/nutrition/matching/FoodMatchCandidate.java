package com.teukgeupjeonsa.backend.nutrition.matching;

import com.teukgeupjeonsa.backend.food.Food;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_match_candidates", indexes = {
        @Index(name = "idx_food_match_candidates_normalized", columnList = "normalized_menu_name"),
        @Index(name = "idx_food_match_candidates_status", columnList = "status"),
        @Index(name = "idx_food_match_candidates_review", columnList = "status, occurrence_count, score")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FoodMatchCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "raw_menu_name", nullable = false, length = 200)
    private String rawMenuName;
    @Column(name = "normalized_menu_name", nullable = false, length = 200)
    private String normalizedMenuName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;
    @Column(name = "matched_food_name", length = 200)
    private String matchedFoodName;
    @Column(name = "match_type", nullable = false, length = 40)
    private String matchType;
    @Column(nullable = false)
    private Double score;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private FoodMatchEnums.CandidateConfidence confidence;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private FoodMatchEnums.CandidateStatus status;
    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;
    @Column(name = "default_serving_gram")
    private Double defaultServingGram;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void onCreate(){ LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}
