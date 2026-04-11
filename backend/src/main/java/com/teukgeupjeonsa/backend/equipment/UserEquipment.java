package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_equipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(length = 80)
    private String customEquipmentName;
}
