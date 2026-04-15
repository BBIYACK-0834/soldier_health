package com.teukgeupjeonsa.backend.mealcrawler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_menu", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_menu_infid_date", columnNames = {"inf_id", "meal_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 120)
    private String sourceName;

    @Column(name = "inf_id", nullable = false, length = 30)
    private String infId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(columnDefinition = "TEXT")
    private String breakfast;

    @Column(columnDefinition = "TEXT")
    private String lunch;

    @Column(columnDefinition = "TEXT")
    private String dinner;

    @Column(name = "breakfast_kcal")
    private Integer breakfastKcal;

    @Column(name = "lunch_kcal")
    private Integer lunchKcal;

    @Column(name = "dinner_kcal")
    private Integer dinnerKcal;

    @Column(name = "total_kcal")
    private Integer totalKcal;

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
