package com.teukgeupjeonsa.backend.equipment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class UnitGymDatasetDtos {

    @Getter
    @Builder
    public static class DatasetResponse {
        private Long id;
        private Long unitId;
        private String unitName;
        private String datasetName;
        private String description;
        private Long createdByUserId;
        private List<EquipmentResponse> equipments;
        private List<String> customEquipmentNames;
    }

    @Getter
    @Setter
    public static class SaveDatasetRequest {
        private String datasetName;
        private String description;
        private List<Long> equipmentIds;
        private List<String> customEquipmentNames;
    }
}
