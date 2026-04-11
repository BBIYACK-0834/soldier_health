package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserEquipmentRepository userEquipmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getEquipments() {
        return equipmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<EquipmentResponse> saveMyEquipments(Long userId, SaveUserEquipmentsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        userEquipmentRepository.deleteByUser(user);

        List<UserEquipment> saving = new ArrayList<>();

        if (request.getEquipmentIds() != null) {
            List<Equipment> equipments = equipmentRepository.findAllById(request.getEquipmentIds());
            for (Equipment equipment : equipments) {
                saving.add(UserEquipment.builder()
                        .user(user)
                        .equipment(equipment)
                        .build());
            }
        }

        if (request.getCustomEquipmentNames() != null) {
            for (String customName : request.getCustomEquipmentNames()) {
                if (customName != null && !customName.isBlank()) {
                    saving.add(UserEquipment.builder()
                            .user(user)
                            .customEquipmentName(customName.trim())
                            .build());
                }
            }
        }

        userEquipmentRepository.saveAll(saving);
        return getMyEquipments(userId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getMyEquipments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        return userEquipmentRepository.findByUser(user).stream().map(entry -> {
            if (entry.getEquipment() != null) {
                return toResponse(entry.getEquipment());
            }
            return EquipmentResponse.builder()
                    .id(entry.getId())
                    .name(entry.getCustomEquipmentName())
                    .category("CUSTOM")
                    .isDefault(false)
                    .build();
        }).toList();
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .isDefault(equipment.getIsDefault())
                .build();
    }
}
