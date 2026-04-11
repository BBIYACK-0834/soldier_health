package com.teukgeupjeonsa.backend.equipment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false)
    private Boolean isDefault;
}
