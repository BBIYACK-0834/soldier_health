package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.user.BranchType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "military_units")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String unitCode;

    @Column(nullable = false, length = 100)
    private String unitName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchType branchType;

    @Column(length = 80)
    private String regionName;

    @Column(length = 120)
    private String dataSourceKey;

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
