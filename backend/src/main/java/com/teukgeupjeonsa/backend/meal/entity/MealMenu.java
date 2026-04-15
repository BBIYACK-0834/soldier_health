package com.teukgeupjeonsa.backend.meal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "meal_menus",
        uniqueConstraints = @UniqueConstraint(name = "uk_meal_menu_service_date", columnNames = {"service_code", "meal_date"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, length = 80)
    private String serviceCode;

    @Column(name = "source_name", length = 120)
    private String sourceName;

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
