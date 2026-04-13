package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
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
    private final MilitaryUnitRepository militaryUnitRepository;
    private final UnitGymDatasetRepository unitGymDatasetRepository;
    private final UnitGymDatasetItemRepository unitGymDatasetItemRepository;

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getEquipments() {
        return equipmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<EquipmentResponse> saveMyEquipments(Long userId, SaveUserEquipmentsRequest request) {
        User user = getUser(userId);

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
        User user = getUser(userId);

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

    @Transactional(readOnly = true)
    public List<UnitGymDatasetDtos.DatasetResponse> getUnitGymDatasets(Long unitId) {
        MilitaryUnit unit = militaryUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("부대를 찾을 수 없습니다."));

        return unitGymDatasetRepository.findByUnitOrderByIdDesc(unit).stream()
                .map(this::toDatasetResponse)
                .toList();
    }

    @Transactional
    public UnitGymDatasetDtos.DatasetResponse saveUnitGymDataset(Long userId, Long unitId, UnitGymDatasetDtos.SaveDatasetRequest request) {
        User user = getUser(userId);
        MilitaryUnit unit = militaryUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("부대를 찾을 수 없습니다."));

        UnitGymDataset dataset = UnitGymDataset.builder()
                .unit(unit)
                .datasetName((request.getDatasetName() == null || request.getDatasetName().isBlank()) ? "기본 헬스장" : request.getDatasetName().trim())
                .description(request.getDescription())
                .createdByUserId(user.getId())
                .build();
        UnitGymDataset saved = unitGymDatasetRepository.save(dataset);

        replaceDatasetItems(saved, request.getEquipmentIds(), request.getCustomEquipmentNames());
        return toDatasetResponse(saved);
    }

    @Transactional
    public UnitGymDatasetDtos.DatasetResponse updateUnitGymDataset(Long userId, Long datasetId, UnitGymDatasetDtos.SaveDatasetRequest request) {
        getUser(userId);
        UnitGymDataset dataset = unitGymDatasetRepository.findById(datasetId)
                .orElseThrow(() -> new EntityNotFoundException("헬스장 데이터셋을 찾을 수 없습니다."));

        if (request.getDatasetName() != null && !request.getDatasetName().isBlank()) {
            dataset.setDatasetName(request.getDatasetName().trim());
        }
        dataset.setDescription(request.getDescription());

        replaceDatasetItems(dataset, request.getEquipmentIds(), request.getCustomEquipmentNames());
        return toDatasetResponse(dataset);
    }

    @Transactional
    public List<EquipmentResponse> applyUnitGymDatasetToMe(Long userId, Long datasetId) {
        User user = getUser(userId);
        UnitGymDataset dataset = unitGymDatasetRepository.findById(datasetId)
                .orElseThrow(() -> new EntityNotFoundException("헬스장 데이터셋을 찾을 수 없습니다."));

        List<UnitGymDatasetItem> items = unitGymDatasetItemRepository.findByDataset(dataset);
        List<Long> equipmentIds = items.stream()
                .filter(item -> item.getEquipment() != null)
                .map(item -> item.getEquipment().getId())
                .toList();
        List<String> customNames = items.stream()
                .map(UnitGymDatasetItem::getCustomEquipmentName)
                .filter(name -> name != null && !name.isBlank())
                .toList();

        SaveUserEquipmentsRequest request = new SaveUserEquipmentsRequest();
        request.setEquipmentIds(equipmentIds);
        request.setCustomEquipmentNames(customNames);
        return saveMyEquipments(user.getId(), request);
    }

    private void replaceDatasetItems(UnitGymDataset dataset, List<Long> equipmentIds, List<String> customEquipmentNames) {
        unitGymDatasetItemRepository.deleteByDataset(dataset);

        List<UnitGymDatasetItem> saving = new ArrayList<>();

        if (equipmentIds != null) {
            List<Equipment> equipments = equipmentRepository.findAllById(equipmentIds);
            for (Equipment equipment : equipments) {
                saving.add(UnitGymDatasetItem.builder().dataset(dataset).equipment(equipment).build());
            }
        }

        if (customEquipmentNames != null) {
            for (String customName : customEquipmentNames) {
                if (customName != null && !customName.isBlank()) {
                    saving.add(UnitGymDatasetItem.builder()
                            .dataset(dataset)
                            .customEquipmentName(customName.trim())
                            .build());
                }
            }
        }

        unitGymDatasetItemRepository.saveAll(saving);
    }

    private UnitGymDatasetDtos.DatasetResponse toDatasetResponse(UnitGymDataset dataset) {
        List<UnitGymDatasetItem> items = unitGymDatasetItemRepository.findByDataset(dataset);

        return UnitGymDatasetDtos.DatasetResponse.builder()
                .id(dataset.getId())
                .unitId(dataset.getUnit().getId())
                .unitName(dataset.getUnit().getUnitName())
                .datasetName(dataset.getDatasetName())
                .description(dataset.getDescription())
                .createdByUserId(dataset.getCreatedByUserId())
                .equipments(items.stream()
                        .filter(item -> item.getEquipment() != null)
                        .map(item -> toResponse(item.getEquipment()))
                        .toList())
                .customEquipmentNames(items.stream()
                        .map(UnitGymDatasetItem::getCustomEquipmentName)
                        .filter(name -> name != null && !name.isBlank())
                        .toList())
                .build();
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .isDefault(equipment.getIsDefault())
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
