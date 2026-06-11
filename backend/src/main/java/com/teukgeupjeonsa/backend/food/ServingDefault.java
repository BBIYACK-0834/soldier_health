package com.teukgeupjeonsa.backend.food;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "serving_defaults",
        indexes = @Index(name = "idx_serving_defaults_category", columnList = "category"),
        uniqueConstraints = @UniqueConstraint(name = "uk_serving_defaults_category", columnNames = "category")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServingDefault {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "serving_gram", nullable = false)
    private Double servingGram;

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
