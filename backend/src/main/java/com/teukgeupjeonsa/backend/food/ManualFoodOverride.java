package com.teukgeupjeonsa.backend.food;

import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "manual_food_overrides",
        indexes = {
                @Index(name = "idx_manual_food_overrides_normalized", columnList = "normalized_menu_name"),
                @Index(name = "idx_manual_food_overrides_raw", columnList = "raw_menu_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_manual_food_overrides_normalized", columnNames = "normalized_menu_name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualFoodOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_menu_name", nullable = false, length = 200)
    private String rawMenuName;

    @Column(name = "normalized_menu_name", nullable = false, length = 200)
    private String normalizedMenuName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchConfidence confidence;

    @Column(name = "default_serving_gram")
    private Double defaultServingGram;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.confidence == null) {
            this.confidence = MatchConfidence.HIGH;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
