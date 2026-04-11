package com.teukgeupjeonsa.backend.equipment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EquipmentResponse {
    private Long id;
    private String name;
    private String category;
    private Boolean isDefault;
}
