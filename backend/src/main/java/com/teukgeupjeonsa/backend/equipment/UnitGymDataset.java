package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unit_gym_datasets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitGymDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @Column(nullable = false, length = 80)
    private String datasetName;

    @Column(length = 300)
    private String description;

    @Column
    private Long createdByUserId;
}
