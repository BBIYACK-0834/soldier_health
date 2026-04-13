package com.teukgeupjeonsa.backend.meal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "military_meal", indexes = {
        @Index(name = "idx_military_meal_unit_date", columnList = "unit_name, meal_date"),
        @Index(name = "idx_military_meal_hash", columnList = "raw_row_hash")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_military_meal_source_unit_date", columnNames = {"source_id", "unit_name", "meal_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private DatasetSource source;

    @Column(length = 40)
    private String branch;

    @Column(name = "unit_name", nullable = false, length = 120)
    private String unitName;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(columnDefinition = "TEXT")
    private String breakfast;

    @Column(columnDefinition = "TEXT")
    private String lunch;

    @Column(columnDefinition = "TEXT")
    private String dinner;

    @Column(name = "raw_row_hash", nullable = false, length = 64)
    private String rawRowHash;

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
