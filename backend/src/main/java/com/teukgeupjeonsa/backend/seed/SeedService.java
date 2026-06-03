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
import java.util.Set;

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
        Set<String> savedNames = equipmentRepository.findAll().stream()
                .map(Equipment::getName)
                .collect(java.util.stream.Collectors.toSet());

        List<Equipment> baseEquipments = List.of(
                Equipment.builder().name("푸쉬업 바").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("철봉").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("평행봉").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("딥스 스탠드").category("BODYWEIGHT").isDefault(true).build(),
                Equipment.builder().name("풀업 어시스트 밴드").category("BAND").isDefault(true).build(),
                Equipment.builder().name("덤벨").category("DUMBBELL").isDefault(true).build(),
                Equipment.builder().name("조절식 덤벨").category("DUMBBELL").isDefault(true).build(),
                Equipment.builder().name("덤벨 랙").category("DUMBBELL").isDefault(true).build(),
                Equipment.builder().name("바벨").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("올림픽 바벨").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("EZ바").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("트랩바").category("BARBELL").isDefault(true).build(),
                Equipment.builder().name("중량 원판").category("PLATE").isDefault(true).build(),
                Equipment.builder().name("범퍼 플레이트").category("PLATE").isDefault(true).build(),
                Equipment.builder().name("스미스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("파워랙").category("RACK").isDefault(true).build(),
                Equipment.builder().name("하프랙").category("RACK").isDefault(true).build(),
                Equipment.builder().name("스쿼트랙").category("RACK").isDefault(true).build(),
                Equipment.builder().name("벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("인클라인 벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("디클라인 벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("플랫 벤치").category("BENCH").isDefault(true).build(),
                Equipment.builder().name("케이블 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("듀얼 케이블 크로스오버").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("랫풀다운 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("시티드 로우 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("티바 로우").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("백 익스텐션 벤치").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("어시스트 풀업 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("숄더 프레스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레터럴 레이즈 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("리어델트 플라이 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("체스트 프레스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("인클라인 체스트 프레스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("펙덱 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("딥스 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레그프레스").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("핵스쿼트 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("레그 익스텐션").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("라잉 레그 컬").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("시티드 레그 컬").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("글루트 햄 레이즈").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("힙 쓰러스트 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("힙 어브덕션 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("힙 어덕션 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("스탠딩 카프 레이즈 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("시티드 카프 레이즈 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("프리처 컬 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("암 컬 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("트라이셉스 익스텐션 머신").category("MACHINE").isDefault(true).build(),
                Equipment.builder().name("케틀벨").category("KETTLEBELL").isDefault(true).build(),
                Equipment.builder().name("샌드백").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("샌드벨").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("메디신볼").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("슬램볼").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("배틀로프").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("플라이오 박스").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("스텝 박스").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("TRX").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("짐 링").category("FUNCTIONAL").isDefault(true).build(),
                Equipment.builder().name("저항밴드").category("BAND").isDefault(true).build(),
                Equipment.builder().name("미니밴드").category("BAND").isDefault(true).build(),
                Equipment.builder().name("폼롤러").category("RECOVERY").isDefault(true).build(),
                Equipment.builder().name("마사지볼").category("RECOVERY").isDefault(true).build(),
                Equipment.builder().name("러닝머신").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("사이클").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("스핀바이크").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("로잉머신").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("스텝밀").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("일립티컬").category("CARDIO").isDefault(true).build(),
                Equipment.builder().name("스키에르그").category("CARDIO").isDefault(true).build()
        );

        List<Equipment> missingEquipments = baseEquipments.stream()
                .filter(equipment -> !savedNames.contains(equipment.getName()))
                .toList();

        if (!missingEquipments.isEmpty()) {
            equipmentRepository.saveAll(missingEquipments);
        }
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
                "EZ바",
                "덤벨",
                "벤치",
                "인클라인 벤치",
                "케이블 머신",
                "랫풀다운 머신",
                "시티드 로우 머신",
                "숄더 프레스 머신",
                "체스트 프레스 머신",
                "펙덱 머신",
                "레그프레스",
                "레그 익스텐션",
                "라잉 레그 컬",
                "케틀벨",
                "저항밴드",
                "폼롤러",
                "러닝머신",
                "사이클",
                "로잉머신"
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
