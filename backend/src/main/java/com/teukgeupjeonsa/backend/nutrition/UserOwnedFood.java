package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_owned_foods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOwnedFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 120)
    private String foodName;

    private Integer calories;
    private Double proteinG;
    private Double carbG;
    private Double fatG;
    private Integer quantity;
}
