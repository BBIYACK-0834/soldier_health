package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.equipment.Equipment;
import com.teukgeupjeonsa.backend.equipment.EquipmentRepository;
import com.teukgeupjeonsa.backend.equipment.UnitGymDataset;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetItem;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetItemRepository;
import com.teukgeupjeonsa.backend.equipment.UnitGymDatasetRepository;
import com.teukgeupjeonsa.backend.nutrition.FoodNutrition;
import com.teukgeupjeonsa.backend.nutrition.FoodNutritionRepository;
import com.teukgeupjeonsa.backend.px.PxProduct;
import com.teukgeupjeonsa.backend.px.PxProductRepository;
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
    private final FoodNutritionRepository foodNutritionRepository;
    private final PxProductRepository pxProductRepository;
    private final UnitGymDatasetRepository unitGymDatasetRepository;
    private final UnitGymDatasetItemRepository unitGymDatasetItemRepository;

    @Transactional
    public String seedSampleData() {
        seedBaseEquipments();
        seedNutrition();
        seedPxProducts();
        seedUnitGymDatasets();

        return "개발용 기본 데이터 시드 완료 - 부대/식단 데이터는 국방부 OpenAPI 수집 결과를 사용합니다.";
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

    private void seedNutrition() {
        if (foodNutritionRepository.count() > 0) {
            return;
        }

        foodNutritionRepository.saveAll(List.of(
                fn("쌀밥", 300, 6.0, 66.0, 1.0),
                fn("계란말이", 180, 12.0, 4.0, 12.0),
                fn("김치", 20, 1.0, 3.0, 0.3),
                fn("우유", 125, 6.0, 10.0, 6.0),
                fn("닭볶음탕", 260, 23.0, 9.0, 14.0),
                fn("두부조림", 170, 14.0, 8.0, 8.0),
                fn("돼지고기볶음", 300, 20.0, 12.0, 18.0),
                fn("된장국", 80, 6.0, 7.0, 3.0),
                fn("닭가슴살", 140, 28.0, 0.0, 2.0)
        ));
    }

    private void seedPxProducts() {
        if (pxProductRepository.count() > 0) {
            return;
        }

        pxProductRepository.saveAll(List.of(
                PxProduct.builder()
                        .productName("참치캔")
                        .brandName("동원")
                        .category("PROTEIN")
                        .calories(180)
                        .proteinG(25.0)
                        .carbG(0.0)
                        .fatG(8.0)
                        .isActive(true)
                        .build(),
                PxProduct.builder()
                        .productName("프로틴 드링크")
                        .brandName("빙그레")
                        .category("PROTEIN")
                        .calories(150)
                        .proteinG(20.0)
                        .carbG(8.0)
                        .fatG(3.0)
                        .isActive(true)
                        .build(),
                PxProduct.builder()
                        .productName("멸균우유")
                        .brandName("서울우유")
                        .category("DAIRY")
                        .calories(125)
                        .proteinG(6.0)
                        .carbG(10.0)
                        .fatG(6.0)
                        .isActive(true)
                        .build()
        ));
    }

    private FoodNutrition fn(String name, int calories, double protein, double carb, double fat) {
        return FoodNutrition.builder()
                .foodName(name)
                .servingUnit("1인분")
                .calories(calories)
                .proteinG(protein)
                .carbG(carb)
                .fatG(fat)
                .build();
    }
}