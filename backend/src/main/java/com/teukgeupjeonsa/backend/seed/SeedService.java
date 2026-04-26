package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.equipment.Equipment;
import com.teukgeupjeonsa.backend.equipment.EquipmentRepository;
import com.teukgeupjeonsa.backend.equipment.UnitGymDataset;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetItem;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetItemRepository;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetRepository;
import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final MilitaryUnitRepository militaryUnitRepository;
    private final EquipmentRepository equipmentRepository;
    private final UnitGymDatasetRepository unitGymDatasetRepository;
    private final UnitGymDatasetItemRepository unitGymDatasetItemRepository;

    @Transactional
    public String seedSampleData() {
        seedBaseEquipments();
        seedUnitGymDatasets();

        return "기준 장비 데이터 시드 완료 - 부대/식단/영양/PX 데이터는 DB 수집 결과를 사용합니다.";
    }

    @Transactional
    public String seedSampleMeals() {
        return "샘플 식단 시드는 비활성화되었습니다. 식단 데이터는 국방부 OpenAPI 수집 API를 사용하세요.";
    }

    private void seedBaseEquipments() {
        if (equipmentRepository.count() > 0) {
            return;
        }

        equipmentRepository.saveAll(List.of(
                Equipment.builder().name("푸쉬업 바").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("철봉").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("평행봉").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("딥스 스탠드").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("덤벨").category("DUMBBELL").isDefault(true).build(),
                Equipment.builder().name("조절식 덤벨").category("DUMBBELL").isDefault(true).build(),
                Equipment.builder().name("바벨").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("EZ바").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("스미스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("파워랙").category("RACK").isDefault(true).build(),
                Equipment.builder().name("하프랙").category("RACK").isDefault(true).build(),
                Equipment.builder().name("벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("인클라인 벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("디클라인 벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("케이블 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("랫풀다운 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("시티드 로우 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("숄더 프레스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("체스트 프레스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("펙덱 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레그프레스").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레그 익스텐션").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레그 컬").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("힙 어브덕션 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("힙 어덕션 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("케틀벨").category("KETTLEBELL").isDefault(true).build(),
                Equipment.builder().name("메디신볼").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("배틀로프").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("플라이오 박스").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("TRX").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("저항밴드").category("BAND").isDefault(true).build(),
                Equipment.builder().name("폼롤러").category("RECOVERY").isDefault(true).build(),
                Equipment.builder().name("러닝머신").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("사이클").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("로잉머신").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("스텝밀").category("CARDIO").isDefault(true).build()
        ));
    }

    private void seedUnitGymDatasets() {
        if (unitGymDatasetRepository.count() > 0) {
            return;
        }

        List<MilitaryUnit> units = militaryUnitRepository.findAll();
        if (units.isEmpty()) {
            return;
        }

        List<Equipment> baseEquipments = equipmentRepository.findAll();
        List<String> defaults = List.of(
                "파워랙",
                "바벨",
                "덤벨",
                "벤치",
                "케이블 머신",
                "랫풀다운 머신",
                "레그프레스",
                "러닝머신"
        );

        for (MilitaryUnit unit : units) {
            UnitGymDataset dataset = unitGymDatasetRepository.save(UnitGymDataset.builder()
                    .unit(unit)
                    .datasetName(unit.getUnitName() + " 공용 헬스장")
                    .description("해당 부대 이용자들이 함께 관리하는 기본 기구 데이터셋")
                    .createdByUserId(0L)
                    .build());

            List<UnitGymDatasetItem> items = new ArrayList<>();
            for (Equipment equipment : baseEquipments) {
                if (defaults.contains(equipment.getName())) {
                    items.add(UnitGymDatasetItem.builder()
                            .dataset(dataset)
                            .equipment(equipment)
                            .build());
                }
            }

            unitGymDatasetItemRepository.saveAll(items);
        }
    }

}
