package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "military_menu_profiles",
        indexes = @Index(name = "idx_military_menu_profiles_search", columnList = "search_name"),
        uniqueConstraints = @UniqueConstraint(name = "uk_military_menu_profiles_search", columnNames = "search_name"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryMenuProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, length = 240)
    private String canonicalName;

    @Column(name = "search_name", nullable = false, length = 240)
    private String searchName;

    @Column(length = 80)
    private String category;

    @Column(name = "median_kcal")
    private Double medianKcal;
    @Column(name = "mean_kcal")
    private Double meanKcal;
    @Column(name = "q1_kcal")
    private Double q1Kcal;
    @Column(name = "q3_kcal")
    private Double q3Kcal;
    @Column(name = "min_kcal")
    private Double minKcal;
    @Column(name = "max_kcal")
    private Double maxKcal;
    @Column(name = "valid_kcal_count")
    private Integer validKcalCount;
    @Column(name = "observation_count")
    private Integer observationCount;
    @Column(name = "unit_count")
    private Integer unitCount;
    @Column(name = "first_date")
    private LocalDate firstDate;
    @Column(name = "last_date")
    private LocalDate lastDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchConfidence confidence;

    @Column(name = "review_reason", length = 240)
    private String reviewReason;
}
