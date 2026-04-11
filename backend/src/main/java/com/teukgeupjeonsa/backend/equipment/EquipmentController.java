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
}
