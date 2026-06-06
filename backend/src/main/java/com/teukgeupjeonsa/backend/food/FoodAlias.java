package com.teukgeupjeonsa.backend.food;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "food_aliases",
        indexes = {
                @Index(name = "idx_food_aliases_alias_name", columnList = "alias_name"),
                @Index(name = "idx_food_aliases_search_name", columnList = "search_name"),
                @Index(name = "idx_food_aliases_original_name", columnList = "original_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_food_aliases_food_alias_original", columnNames = {"food_id", "alias_name", "original_name"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(name = "alias_name", nullable = false, length = 300)
    private String aliasName;

    @Column(name = "search_name", nullable = false, length = 300)
    private String searchName;

    @Column(name = "original_name", nullable = false, length = 300)
    private String originalName;

    @Column(length = 80)
    private String category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
