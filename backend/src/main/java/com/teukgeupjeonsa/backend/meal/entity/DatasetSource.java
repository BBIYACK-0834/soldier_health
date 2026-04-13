package com.teukgeupjeonsa.backend.meal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_source", indexes = {
        @Index(name = "idx_dataset_source_title", columnList = "source_title")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dataset_source_url", columnNames = "source_url")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_title", nullable = false, length = 255)
    private String sourceTitle;

    @Column(name = "source_url", nullable = false, length = 700)
    private String sourceUrl;

    @Column(name = "download_url", length = 700)
    private String downloadUrl;

    @Column(length = 100)
    private String provider;

    @Column(length = 20)
    private String format;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Builder.Default
    private Boolean active = true;

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
