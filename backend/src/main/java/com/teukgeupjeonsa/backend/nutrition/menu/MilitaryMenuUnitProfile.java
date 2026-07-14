package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "military_menu_unit_profiles",
        indexes = {
                @Index(name = "idx_military_menu_unit_profile_unit", columnList = "unit_code"),
                @Index(name = "idx_military_menu_unit_profile_menu", columnList = "menu_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_military_menu_unit_profile", columnNames = {"menu_id", "unit_code"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryMenuUnitProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private MilitaryMenuProfile menu;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;
    @Column(name = "median_kcal")
    private Double medianKcal;
    @Column(name = "q1_kcal")
    private Double q1Kcal;
    @Column(name = "q3_kcal")
    private Double q3Kcal;
    @Column(name = "valid_kcal_count")
    private Integer validKcalCount;
    @Column(name = "observation_count")
    private Integer observationCount;
    @Column(name = "first_date")
    private LocalDate firstDate;
    @Column(name = "last_date")
    private LocalDate lastDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchConfidence confidence;
}
