package com.teukgeupjeonsa.backend.px;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "px_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PxProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(length = 80)
    private String brandName;

    @Column(length = 40)
    private String category;

    private Integer calories;
    private Double proteinG;
    private Double carbG;
    private Double fatG;

    @Column(nullable = false)
    private Boolean isActive;
}
