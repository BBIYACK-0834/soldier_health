package com.teukgeupjeonsa.backend.equipment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unit_gym_dataset_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitGymDatasetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id")
    private UnitGymDataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(length = 80)
    private String customEquipmentName;
}
