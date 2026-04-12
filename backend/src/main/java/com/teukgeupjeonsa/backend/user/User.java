package com.teukgeupjeonsa.backend.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    private Double heightCm;

    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkoutLevel workoutLevel;

    private Integer workoutDaysPerWeek;

    private Integer preferredWorkoutMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BranchType branchType;

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
