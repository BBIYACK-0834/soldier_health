# Food Matching Pipeline

군 급식 OpenAPI 메뉴명과 공공데이터포털 식품영양 엑셀을 1:1로 수동 매칭하지 않기 위한 백엔드 파이프라인입니다.

런타임에서는 군 식단 이력에서 만든 부대별 메뉴 프로필을 먼저 확인하고, 없으면 전체 군 메뉴 중앙값, 마지막으로 아래 일반 식품 매칭 파이프라인을 사용합니다. 군 식단 프로필은 `./gradlew importMilitaryMenus`로 `military_menu_data.xlsx`에서 별도 적재합니다.

## Flow

```text
공공데이터 원본 xlsx
→ PublicFoodXlsxNormalizer
→ food_master_clean_100g / food_alias / serving_defaults / review_needed
→ FoodXlsxImporter
→ meal_menus 빈도 분석
→ 후보 생성
→ 고확신 자동 승인
→ 빈도 높은 애매한 메뉴만 검수
→ manual_food_overrides 누적
```

## 1. PublicFoodXlsxNormalizer

`PublicFoodXlsxNormalizer`는 공공데이터포털 원본 컬럼을 기존 `FoodXlsxImporter`가 읽는 시트로 변환합니다.

입력 주요 컬럼:

| 원본 | 출력 |
| --- | --- |
| DESC_KOR | representative_name |
| GROUP_NAME | display_category |
| SERVING_SIZE | 100g 환산 기준 |
| NUTR_CONT1 | kcal_100g |
| NUTR_CONT2 | carb_100g |
| NUTR_CONT3 | protein_100g |
| NUTR_CONT4 | fat_100g |
| NUTR_CONT5 | sugar_g |
| NUTR_CONT6 | sodium_mg |
| NUTR_CONT7 | cholesterol_mg |
| NUTR_CONT8 | saturated_fat_g |
| NUTR_CONT9 | trans_fat_g |

출력 시트:

1. `food_master_clean_100g`
2. `food_alias`
3. `serving_defaults`
4. `review_needed`

정제 규칙:

- 첫 행이 설명 행이어도 `DESC_KOR`/`SERVING_SIZE` 헤더 행을 찾아 사용합니다.
- `SERVING_SIZE`가 없거나 0 이하인 행은 제외합니다.
- `SERVING_SIZE > 2000g`인 행은 `quality_flag=OUTLIER`로 표시하고 `review_needed`에 기록합니다.
- 영양성분은 모두 `값 / SERVING_SIZE * 100`으로 100g 기준 환산합니다.
- 같은 식품명은 normalized key로 묶고, 영양성분은 median으로 대표값을 계산합니다.
- 원본 `DESC_KOR`는 `food_alias`에 누적합니다.


### 이미 정제된 `foods_final_user_friendly_100g.xlsx` 형식

사용자 친화 정제본은 아래 컬럼명을 그대로 지원합니다. 이 파일은 `FoodXlsxImporter`가 직접 읽을 수 있고, `PublicFoodXlsxNormalizer`에 넣어도 importer 호환 시트로 재출력됩니다.

| 정제본 컬럼 | 앱 매핑 |
| --- | --- |
| food_name | representative_name / foods.name |
| display_category | foods.category |
| main_original_group | 참고용 원본군 |
| basis | `100g`이면 100g 기준으로 처리 |
| calorie_kcal | foods.calorie |
| carbohydrate_g | foods.carbohydrate |
| protein_g | foods.protein |
| fat_g | foods.fat |
| sugar_g | foods.sugar |
| sodium_mg | foods.sodium |
| cholesterol_mg | foods.cholesterol |
| saturated_fat_g | foods.saturated_fat |
| trans_fat_g | foods.trans_fat |
| source_count | 검색/정렬 가중치 |
| merged_raw_name_count | 참고용 병합 원본 수 |
| merge_method | 정제 방식 메모 |
| confidence | 품질 검수 참고 |
| research_years | 참고용 조사연도 |
| source_name_samples | `food_alias` 후보 샘플 |

`food_alias` 시트는 `raw_food_name`, `final_food_name`, `display_category` 컬럼을 지원합니다. `food_id`가 없어도 `final_food_name` 또는 `raw_food_name`으로 대표 식품을 찾아 alias를 연결합니다.

## 2. Occurrence rebuild

실제 검수 대상은 전체 식품 후보가 아니라 `meal_menus`에 자주 등장한 메뉴입니다.

```http
POST /api/admin/foods/matching/occurrences/rebuild
```

`MealMenuOccurrenceService`가 breakfast/lunch/dinner를 메뉴 단위로 분리하고, normalized name별 등장 횟수와 최근 등장일을 `meal_menu_occurrences`에 저장합니다.

## 3. Candidate rebuild

```http
POST /api/admin/foods/matching/candidates/rebuild
```

`FoodMatchCandidateService`가 `FoodMatcher`와 `CompositeFoodEstimator`를 재사용해 후보를 만들고 `food_match_candidates`에 저장합니다.

점수 정책:

- manual override exact: `1.00`
- food alias exact: `0.99`
- foods.search_name exact: `0.98`
- normalized 포함 관계: `0.80~0.94`
- 핵심 토큰 매칭: `0.70~0.90`
- 유사도 매칭: `0.65~0.85`
- 복합 음식 추정: `0.60~0.82` 범위 후보로 취급

자동 처리:

- `score >= 0.93`이고 짧은 위험 메뉴가 아니면 `AUTO_APPROVED` 후 `manual_food_overrides`에 저장합니다.
- `0.78 <= score < 0.93`이고 `occurrence_count >= 3`이면 `NEEDS_REVIEW`입니다.
- 그 외 후보는 검수 화면에서 기본 숨김 처리됩니다.

짧고 애매한 `국`, `탕`, `전`, `차`, `빵`은 자동 승인하지 않습니다. `밥`, `쌀밥`, `잡곡밥`, `김치`, `배추김치`는 예외입니다.

## 4. Review queue

```http
GET /api/admin/foods/matching/review?limit=100
```

기본 노출 조건:

- `status = NEEDS_REVIEW`
- `occurrence_count >= 3`
- `score >= 0.78`

정렬은 `occurrence_count DESC`, `score DESC`, `updatedAt DESC`입니다.

승인/거절:

```http
POST /api/admin/foods/matching/candidates/{id}/approve
POST /api/admin/foods/matching/candidates/{id}/reject
```

승인 시 `manual_food_overrides`에 `confidence=HIGH`로 저장됩니다. 이후 런타임 `FoodMatcher`는 기존처럼 manual override를 최우선으로 사용합니다.

## 5. Official kcal calibration

`MealNutritionService`는 official kcal이 있고, 매칭 비율이 `0.6` 이상이며, `official / calculated` scale이 `0.6~1.6` 사이일 때 음식별 kcal/탄수화물/단백질/지방을 같은 비율로 보정합니다. 정수 반올림 후 남는 kcal 오차와 음식별 비율 오차는 마지막 유효 항목에서 보정해 합계가 각각 official kcal과 100%가 되도록 합니다.
