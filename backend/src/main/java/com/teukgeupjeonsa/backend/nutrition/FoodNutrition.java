package com.teukgeupjeonsa.backend.nutrition;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_nutrition")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String foodName;

    @Column(length = 40)
    private String servingUnit;

    private Integer calories;
    private Double proteinG;
    private Double carbG;
    private Double fatG;
}
