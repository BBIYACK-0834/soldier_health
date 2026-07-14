package com.teukgeupjeonsa.backend.nutrition.menu;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "military_menu_daily_profiles",
        indexes = @Index(name = "idx_military_menu_daily_lookup",
                columnList = "unit_code,meal_date,meal_type,search_name"),
        uniqueConstraints = @UniqueConstraint(name = "uk_military_menu_daily_lookup",
                columnNames = {"unit_code", "meal_date", "meal_type", "search_name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryMenuDailyProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;
    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;
    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;
    @Column(name = "search_name", nullable = false, length = 240)
    private String searchName;
    @Column(name = "canonical_name", nullable = false, length = 240)
    private String canonicalName;
    @Column(name = "calorie_kcal", nullable = false)
    private Double calorieKcal;
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;
}
