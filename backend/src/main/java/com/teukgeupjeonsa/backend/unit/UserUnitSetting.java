package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_unit_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUnitSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @Column(nullable = false)
    private Boolean isPrimary;
}
