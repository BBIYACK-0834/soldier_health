package com.teukgeupjeonsa.backend.collector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "unit_api_sources", indexes = {
        @Index(name = "idx_unit_api_sources_service_name", columnList = "service_name", unique = true),
        @Index(name = "idx_unit_api_sources_unit_name", columnList = "unit_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitApiSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_name", nullable = false, length = 120)
    private String unitName;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "service_name", nullable = false, length = 120, unique = true)
    private String serviceName;

    @Column(name = "open_api_base_url", nullable = false, length = 255)
    private String openApiBaseUrl;

    @Column(name = "provider", length = 120)
    private String provider;

    @Column(name = "source_updated_at")
    private LocalDate sourceUpdatedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (!this.active) {
            this.active = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
