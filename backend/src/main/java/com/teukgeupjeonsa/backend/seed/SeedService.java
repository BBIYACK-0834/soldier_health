package com.teukgeupjeonsa.backend.seed;

import com.teukgeupjeonsa.backend.equipment.*;
import com.teukgeupjeonsa.backend.meal.MealDay;
import com.teukgeupjeonsa.backend.meal.MealDayRepository;
import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final MilitaryUnitRepository militaryUnitRepository;
    private final EquipmentRepository equipmentRepository;
    private final MealDayRepository mealDayRepository;
    private final MealMenuRepository mealMenuRepository;
    private final FoodNutritionRepository foodNutritionRepository;
    private final PxProductRepository pxProductRepository;
    private final UnitGymDatasetRepository unitGymDatasetRepository;
    private final UnitGymDatasetItemRepository unitGymDatasetItemRepository;

    @Transactional
    public String seedSampleData() {
        seedBase();
        seedSampleMealsInternal();
        seedSampleMealMenus();
        seedNutrition();
        seedPxProducts();
        seedUnitGymDatasets();
        return "샘플 데이터 시드 완료";
    }

    @Transactional
    public String seedSampleMeals() {
        seedBase();
        seedSampleMealsInternal();
        seedSampleMealMenus();
        return "샘플 식단 시드 완료";
    }

    private void seedBase() {
        if (militaryUnitRepository.count() == 0) {
            militaryUnitRepository.saveAll(List.of(
                    MilitaryUnit.builder().unitCode("ARMY-001").unitName("육군 제1사단").branchType(BranchType.ARMY).regionName("파주").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_1570").build(),
                    MilitaryUnit.builder().unitCode("ARMY-TRN").unitName("육군훈련소").branchType(BranchType.ARMY).regionName("논산").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_ATC").build(),
                    MilitaryUnit.builder().unitCode("AIR-EDU").unitName("공군 교육사령부").branchType(BranchType.AIR_FORCE).regionName("진주").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_5861").build(),
                    MilitaryUnit.builder().unitCode("MAR-001").unitName("해병대 제1사단").branchType(BranchType.MARINES).regionName("포항").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_3389").build(),
                    MilitaryUnit.builder().unitCode("NAVY-JH").unitName("해군 진해기지사령부").branchType(BranchType.NAVY).regionName("창원").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_1691").build(),
                    MilitaryUnit.builder().unitCode("ARMY-017").unitName("육군 제17사단").branchType(BranchType.ARMY).regionName("인천").dataSourceKey("DS_TB_MNDT_DATEBYMLSVC_STANDARD").build()
            ));
        }
        synchronizeUnitDataSourceKeys();

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

    // 현재 시드 부대 수가 적어 식단 기반 매칭 후보 풀이 제한될 수 있음.
    // 신규 부대가 추가되면 unitCode -> serviceCode 매핑만 확장하면 매칭 대상에 포함된다.
    private void synchronizeUnitDataSourceKeys() {
        Map<String, String> unitCodeToServiceCode = new HashMap<>();
        unitCodeToServiceCode.put("ARMY-001", "DS_TB_MNDT_DATEBYMLSVC_1570");
        unitCodeToServiceCode.put("ARMY-TRN", "DS_TB_MNDT_DATEBYMLSVC_ATC");
        unitCodeToServiceCode.put("AIR-EDU", "DS_TB_MNDT_DATEBYMLSVC_5861");
        unitCodeToServiceCode.put("MAR-001", "DS_TB_MNDT_DATEBYMLSVC_3389");
        unitCodeToServiceCode.put("NAVY-JH", "DS_TB_MNDT_DATEBYMLSVC_1691");
        unitCodeToServiceCode.put("ARMY-017", "DS_TB_MNDT_DATEBYMLSVC_STANDARD");

        List<MilitaryUnit> units = militaryUnitRepository.findAll();
        for (MilitaryUnit unit : units) {
            String mappedServiceCode = unitCodeToServiceCode.get(unit.getUnitCode());
            if (mappedServiceCode == null) {
                continue;
            }
            if (!mappedServiceCode.equals(unit.getDataSourceKey())) {
                unit.setDataSourceKey(mappedServiceCode);
                militaryUnitRepository.save(unit);
            }
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

    // meal_menus + military_units.data_source_key 경로를 온보딩 식단표 찾기의 기준 데이터로 사용한다.
    // 샘플 데이터는 부대별 메뉴 차이를 일부러 분리해 2~3개 메뉴 선택 시 후보가 줄어들도록 구성한다.
    private void seedSampleMealMenus() {
        List<MilitaryUnit> units = militaryUnitRepository.findAll();
        if (units.isEmpty()) {
            return;
        }

        LocalDate baseDate = LocalDate.now();
        LocalDate nextDate = baseDate.plusDays(1);

        for (MilitaryUnit unit : units) {
            String serviceCode = unit.getDataSourceKey();
            if (serviceCode == null || serviceCode.isBlank()) {
                continue;
            }

            if (mealMenuRepository.findByServiceCodeAndMealDate(serviceCode, baseDate).isEmpty()) {
                mealMenuRepository.save(MealMenu.builder()
                        .serviceCode(serviceCode)
                        .sourceName("sample-seed")
                        .mealDate(baseDate)
                        .breakfast("쌀밥, 소고기미역국, 계란말이, 김치")
                        .lunch(buildLunchByUnit(unit.getUnitCode()))
                        .dinner(buildDinnerByUnit(unit.getUnitCode()))
                        .breakfastKcal(700)
                        .lunchKcal(900)
                        .dinnerKcal(850)
                        .totalKcal(2450)
                        .build());
            }

            if (mealMenuRepository.findByServiceCodeAndMealDate(serviceCode, nextDate).isEmpty()) {
                mealMenuRepository.save(MealMenu.builder()
                        .serviceCode(serviceCode)
                        .sourceName("sample-seed")
                        .mealDate(nextDate)
                        .breakfast("쌀밥, 북어국, 햄야채볶음, 김치")
                        .lunch("쌀밥, 제육볶음, 콩나물무침, 배추김치")
                        .dinner("쌀밥, 닭갈비, 어묵볶음, 깍두기")
                        .breakfastKcal(680)
                        .lunchKcal(920)
                        .dinnerKcal(780)
                        .totalKcal(2380)
                        .build());
            }
        }
    }

    private String buildLunchByUnit(String unitCode) {
        return switch (unitCode) {
            case "ARMY-001" -> "쌀밥, 닭볶음탕, 두부조림, 깍두기";
            case "ARMY-TRN" -> "쌀밥, 불고기볶음우동, 소불고기당면볶음, 깍두기";
            case "AIR-EDU" -> "쌀밥, 함박스테이크, 감자샐러드, 김치";
            case "MAR-001" -> "쌀밥, 돼지김치찜, 계란찜, 깍두기";
            case "NAVY-JH" -> "쌀밥, 오징어볶음, 콩나물국, 김치";
            case "ARMY-017" -> "쌀밥, 카레라이스, 만두튀김, 배추김치";
            default -> "쌀밥, 제육볶음, 콩나물무침, 배추김치";
        };
    }

    private String buildDinnerByUnit(String unitCode) {
        return switch (unitCode) {
            case "ARMY-001" -> "쌀밥, 등심돈가스&소스, 김자반, 굴림만두전골";
            case "ARMY-TRN" -> "쌀밥, 불고기볶음우동, 동파육, 김치";
            case "AIR-EDU" -> "쌀밥, 함박스테이크, 감자돈육짜글이, 김자반";
            case "MAR-001" -> "쌀밥, 생선가스&콘소스, 햄전&케찹, 깍두기";
            case "NAVY-JH" -> "쌀밥, 소불고기당면볶음, 굴림만두전골, 김치";
            case "ARMY-017" -> "쌀밥, 돼지고기볶음, 된장국, 김치";
            default -> "쌀밥, 닭갈비, 어묵볶음, 깍두기";
        };
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
