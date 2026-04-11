package com.teukgeupjeonsa.backend.equipment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveUserEquipmentsRequest {
    private List<Long> equipmentIds;
    private List<String> customEquipmentNames;
}
