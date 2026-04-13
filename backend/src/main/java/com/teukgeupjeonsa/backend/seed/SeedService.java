package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.equipment.*;
import com.teukgeupjeonsa.backend.meal.MealDay;
import com.teukgeupjeonsa.backend.meal.MealDayRepository;
import com.teukgeupjeonsa.backend.nutrition.FoodNutrition;
import com.teukgeupjeonsa.backend.nutrition.FoodNutritionRepository;
import com.teukgeupjeonsa.backend.px.PxProduct;
import com.teukgeupjeonsa.backend.px.PxProductRepository;
import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.MilitaryUnitRepository;
import com.teukgeupjeonsa.backend.user.BranchType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final MilitaryUnitRepository militaryUnitRepository;
    private final EquipmentRepository equipmentRepository;
    private final MealDayRepository mealDayRepository;
    private final FoodNutritionRepository foodNutritionRepository;
    private final PxProductRepository pxProductRepository;
    private final UnitGymDatasetRepository unitGymDatasetRepository;
    private final UnitGymDatasetItemRepository unitGymDatasetItemRepository;

    @Transactional
    public String seedSampleData() {
        seedBase();
        seedSampleMealsInternal();
        seedNutrition();
        seedPxProducts();
        seedUnitGymDatasets();
        return "샘플 데이터 시드 완료";
    }

    @Transactional
    public String seedSampleMeals() {
        seedBase();
        seedSampleMealsInternal();
        return "샘플 식단 시드 완료";
    }

    private void seedBase() {
        if (militaryUnitRepository.count() == 0) {
            militaryUnitRepository.saveAll(List.of(
                    MilitaryUnit.builder().unitCode("ARMY-001").unitName("육군 제1사단").branchType(BranchType.ARMY).regionName("파주").dataSourceKey("sample-army-1").build(),
                    MilitaryUnit.builder().unitCode("ARMY-TRN").unitName("육군훈련소").branchType(BranchType.ARMY).regionName("논산").dataSourceKey("sample-army-training").build(),
                    MilitaryUnit.builder().unitCode("AIR-EDU").unitName("공군 교육사령부").branchType(BranchType.AIR_FORCE).regionName("진주").dataSourceKey("sample-air-edu").build(),
                    MilitaryUnit.builder().unitCode("MAR-001").unitName("해병대 제1사단").branchType(BranchType.MARINES).regionName("포항").dataSourceKey("sample-marines-1").build(),
                    MilitaryUnit.builder().unitCode("NAVY-JH").unitName("해군 진해기지사령부").branchType(BranchType.NAVY).regionName("창원").dataSourceKey("sample-navy-jinhae").build(),
                    MilitaryUnit.builder().unitCode("ARMY-017").unitName("육군 제17사단").branchType(BranchType.ARMY).regionName("인천").dataSourceKey("sample-army-17").build()
            ));
        }

        if (equipmentRepository.count() == 0) {
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
        List<String> defaults = List.of("파워랙", "바벨", "덤벨", "벤치", "케이블 머신", "랫풀다운 머신", "레그프레스", "러닝머신");

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

    private void seedSampleMealsInternal() {
        List<MilitaryUnit> units = militaryUnitRepository.findAll();
        if (units.isEmpty()) {
            return;
        }

        for (MilitaryUnit unit : units) {
            if (mealDayRepository.findByUnitAndMealDate(unit, LocalDate.now()).isEmpty()) {
                mealDayRepository.saveAll(List.of(
                        MealDay.builder()
                                .unit(unit)
                                .mealDate(LocalDate.now())
                                .breakfastRaw("쌀밥, 계란말이, 김치, 우유")
                                .lunchRaw("쌀밥, 닭볶음탕, 두부조림, 깍두기")
                                .dinnerRaw("쌀밥, 돼지고기볶음, 된장국, 김치")
                                .breakfastKcal(700).lunchKcal(900).dinnerKcal(850)
                                .sourceName("sample-seed")
                                .build(),
                        MealDay.builder()
                                .unit(unit)
                                .mealDate(LocalDate.now().plusDays(1))
                                .breakfastRaw("쌀밥, 참치김치찌개, 계란후라이")
                                .lunchRaw("쌀밥, 제육볶음, 콩나물무침")
                                .dinnerRaw("쌀밥, 닭가슴살샐러드, 미역국")
                                .breakfastKcal(680).lunchKcal(920).dinnerKcal(780)
                                .sourceName("sample-seed")
                                .build()
                ));
            }
        }
    }

    private void seedNutrition() {
        if (foodNutritionRepository.count() == 0) {
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
    }

    private void seedPxProducts() {
        if (pxProductRepository.count() == 0) {
            pxProductRepository.saveAll(List.of(
                    PxProduct.builder().productName("참치캔").brandName("동원").category("PROTEIN").calories(180).proteinG(25.0).carbG(0.0).fatG(8.0).isActive(true).build(),
                    PxProduct.builder().productName("프로틴 드링크").brandName("빙그레").category("PROTEIN").calories(150).proteinG(20.0).carbG(8.0).fatG(3.0).isActive(true).build(),
                    PxProduct.builder().productName("멸균우유").brandName("서울우유").category("DAIRY").calories(125).proteinG(6.0).carbG(10.0).fatG(6.0).isActive(true).build()
            ));
        }
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
