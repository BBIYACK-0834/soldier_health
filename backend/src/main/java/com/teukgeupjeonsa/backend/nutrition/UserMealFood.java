package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_meal_foods",
        indexes = {
                @Index(name = "idx_user_meal_foods_user_date_meal", columnList = "user_id,meal_date,meal_type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMealFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(nullable = false, length = 200)
    private String foodName;

    private Integer calories;
    private Double proteinG;
    private Double carbG;
    private Double fatG;
    private Double quantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
