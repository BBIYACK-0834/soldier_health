package com.teukgeupjeonsa.backend.nutrition.matching;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_menu_occurrences", indexes = {
        @Index(name = "idx_meal_menu_occurrences_normalized", columnList = "normalized_menu_name"),
        @Index(name = "idx_meal_menu_occurrences_count", columnList = "occurrence_count")
}, uniqueConstraints = @UniqueConstraint(name = "uk_meal_menu_occurrences_normalized", columnNames = "normalized_menu_name"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MealMenuOccurrence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "raw_menu_name", nullable = false, length = 200)
    private String rawMenuName;
    @Column(name = "normalized_menu_name", nullable = false, length = 200)
    private String normalizedMenuName;
    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;
    @Column(name = "last_seen_date")
    private LocalDate lastSeenDate;
    @Column(name = "sample_service_code", length = 80)
    private String sampleServiceCode;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void onCreate(){ LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}
