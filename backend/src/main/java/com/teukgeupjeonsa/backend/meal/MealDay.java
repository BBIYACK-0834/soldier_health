package com.teukgeupjeonsa.backend.meal;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_days", uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "meal_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(columnDefinition = "TEXT")
    private String breakfastRaw;

    @Column(columnDefinition = "TEXT")
    private String lunchRaw;

    @Column(columnDefinition = "TEXT")
    private String dinnerRaw;

    private Integer breakfastKcal;
    private Integer lunchKcal;
    private Integer dinnerKcal;

    @Column(length = 80)
    private String sourceName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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
