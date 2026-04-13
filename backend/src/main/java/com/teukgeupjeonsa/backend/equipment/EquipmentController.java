package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/api/equipments")
    public ApiResponse<List<EquipmentResponse>> getEquipments() {
        return ApiResponse.ok(equipmentService.getEquipments());
    }

    @PostMapping("/api/users/me/equipments")
    public ApiResponse<List<EquipmentResponse>> saveMyEquipments(
            @AuthenticationPrincipal User user,
            @RequestBody SaveUserEquipmentsRequest request
    ) {
        return ApiResponse.ok(equipmentService.saveMyEquipments(user.getId(), request));
    }

    @GetMapping("/api/users/me/equipments")
    public ApiResponse<List<EquipmentResponse>> getMyEquipments(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(equipmentService.getMyEquipments(user.getId()));
    }

    @GetMapping("/api/units/{unitId}/gym-datasets")
    public ApiResponse<List<UnitGymDatasetDtos.DatasetResponse>> getUnitGymDatasets(@PathVariable Long unitId) {
        return ApiResponse.ok(equipmentService.getUnitGymDatasets(unitId));
    }

    @PostMapping("/api/units/{unitId}/gym-datasets")
    public ApiResponse<UnitGymDatasetDtos.DatasetResponse> createUnitGymDataset(
            @AuthenticationPrincipal User user,
            @PathVariable Long unitId,
            @RequestBody UnitGymDatasetDtos.SaveDatasetRequest request
    ) {
        return ApiResponse.ok(equipmentService.saveUnitGymDataset(user.getId(), unitId, request));
    }

    @PutMapping("/api/gym-datasets/{datasetId}")
    public ApiResponse<UnitGymDatasetDtos.DatasetResponse> updateUnitGymDataset(
            @AuthenticationPrincipal User user,
            @PathVariable Long datasetId,
            @RequestBody UnitGymDatasetDtos.SaveDatasetRequest request
    ) {
        return ApiResponse.ok(equipmentService.updateUnitGymDataset(user.getId(), datasetId, request));
    }

    @PostMapping("/api/users/me/equipments/apply-dataset/{datasetId}")
    public ApiResponse<List<EquipmentResponse>> applyDatasetToMe(
            @AuthenticationPrincipal User user,
            @PathVariable Long datasetId
    ) {
        return ApiResponse.ok(equipmentService.applyUnitGymDatasetToMe(user.getId(), datasetId));
    }
}
