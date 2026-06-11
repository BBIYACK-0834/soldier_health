package com.teukgeupjeonsa.backend.food;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "foods",
        indexes = {
                @Index(name = "idx_foods_name", columnList = "name"),
                @Index(name = "idx_foods_search_name", columnList = "search_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_foods_name", columnNames = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "search_name", nullable = false, length = 200)
    private String searchName;

    @Column(length = 80)
    private String category;

    @Column(name = "serving_unit", nullable = false, length = 20)
    private String servingUnit;

    private Double calorie;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private Double sugar;
    private Double sodium;
    private Double cholesterol;
    @Column(name = "saturated_fat")
    private Double saturatedFat;
    @Column(name = "trans_fat")
    private Double transFat;
    @Column(name = "source_count")
    private Integer sourceCount;

    @Column(name = "quality_flag", length = 40)
    private String qualityFlag;

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
