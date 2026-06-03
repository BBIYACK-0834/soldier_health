# developMe — 개발자가 알아야 할 앱 계약

## 1. UI/UX 기준

- 현재 기준 디자인은 사용자가 제공한 **군인 맞춤 다이어트 & 운동 관리 앱 전체 플로우 이미지**입니다.
- 모든 프론트 작업은 아래 톤을 유지합니다.
  - 모바일 앱 단일 화면 중심: `393px` 폭, 둥근 폰 프레임, 상단 상태바, 하단 5탭 내비게이션.
  - 컬러: 올리브 그린 `#52643c`, 다크 그린 `#3f4f30`, 아이보리 배경 `#f7f5ef`, 베이지 카드 `#f0eee7`.
  - 카드 UI: 16px 내외 radius, 얇은 베이지 border, 낮은 shadow.
  - CTA 버튼: 짙은 올리브 배경, 흰색 텍스트, 굵은 글자.
- 공통 디자인 토큰은 `frontend/src/styles/global.css`의 `--tg-*` CSS 변수에서 관리합니다.
- 공통 모바일 프레임은 `frontend/src/components/layout/AppLayout.jsx`와 `AppLayout.module.css`에서 관리합니다.

## 2. 프론트 폴더 구조

기능/페이지 단위로 JSX와 CSS를 같은 기능 폴더에 둡니다.

| 기능 | 위치 | 주요 라우트 |
| --- | --- | --- |
| 온보딩 | `frontend/src/features/onboarding/` | `/onboarding` |
| 인증 | `frontend/src/features/auth/` | `/login`, `/signup` |
| 초기 설정 | `frontend/src/features/setup/` | `/unit/setup`, `/unit/search`, `/unit/select/:unitId`, `/unit/complete`, `/setup/equipment`, `/setup/profile` |
| 홈 | `frontend/src/features/home/` | `/home` |
| 식단/영양 | `frontend/src/features/nutrition/` | `/diet` |
| 운동 | `frontend/src/features/workout/` | `/exercise`, `/exercise/add/equipment`, `/exercise/datasets`, `/exercise/routine/edit` |
| 커뮤니티 | `frontend/src/features/community/` | `/community` |
| 마이페이지 | `frontend/src/features/profile/` | `/mypage` |

공통 컴포넌트는 `frontend/src/components/`, API 클라이언트는 `frontend/src/api/`, 앱 라우터/컨텍스트는 `frontend/src/app/`에 유지합니다.

## 3. API 계약 요약

프론트 API 모듈은 백엔드 컨트롤러의 엔드포인트와 1:1로 맞춥니다.

| 도메인 | 프론트 모듈 | 주요 API |
| --- | --- | --- |
| 인증 | `authApi.js` | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me` |
| 유저 | `userApi.js` | `GET /api/users/me`, `PUT /api/users/me/profile`, `PUT /api/users/me/goals` |
| 부대 | `unitApi.js` | `GET /api/units`, `GET /api/units/search`, `POST /api/units/match-by-meal`, `GET /api/units/meal-options`, `POST/GET /api/users/me/unit` |
| 기구/데이터셋 | `equipmentApi.js` | `GET /api/equipments`, `POST/GET /api/users/me/equipments`, `GET/POST /api/units/{unitId}/gym-datasets`, `POST /api/users/me/equipments/apply-dataset/{datasetId}` |
| 식단 | `mealApi.js` | `GET /api/user/meals/today`, `GET /api/user/meals?date=YYYY-MM-DD` |
| 영양 | `nutritionApi.js` | `GET /api/nutrition/today`, `GET /api/nutrition/recommendation/today`, `GET /api/users/me/owned-foods` |
| PX | `pxApi.js` | `GET /api/px-products` |
| 운동 | `workoutApi.js` | `GET /api/workouts/recommendation/today` |
| 알림 | `alarmApi.js` | `GET /api/alarms/me`, `POST /api/alarms`, `PUT /api/alarms/{id}`, `DELETE /api/alarms/{id}` |
| 커뮤니티 | `communityApi.js` | `GET/POST /api/community/posts`, `GET /api/community/posts/{postId}`, `POST /api/community/posts/{postId}/comments` |

## 4. 변수명/DB 컬럼 기준

- DB 컬럼은 사용자가 제공한 `tg` 스키마의 snake_case를 정본으로 봅니다.
- Java 엔티티/DTO와 프론트 JSON은 camelCase를 사용하되, 의미는 DB 컬럼과 일치시킵니다.
- 대표 매핑:

| DB column | Java/JSON key |
| --- | --- |
| `branch_type` | `branchType` |
| `goal_type` | `goalType` |
| `height_cm` | `heightCm` |
| `weight_kg` | `weightKg` |
| `preferred_workout_minutes` | `preferredWorkoutMinutes` |
| `workout_days_per_week` | `workoutDaysPerWeek` |
| `workout_level` | `workoutLevel` |
| `unit_id` | `unitId` |
| `unit_name` | `unitName` |
| `unit_code` | `unitCode` |
| `region_name` | `regionName` |
| `data_source_key` | `dataSourceKey` |
| `food_name` | `foodName` |
| `product_name` | `productName` |
| `brand_name` | `brandName` |
| `meal_date` | `mealDate` |
| `breakfast_kcal` | `breakfastKcal` |
| `lunch_kcal` | `lunchKcal` |
| `dinner_kcal` | `dinnerKcal` |
| `total_kcal` | `totalKcal` |
| `dataset_name` | `datasetName` |
| `custom_equipment_name` | `customEquipmentName` |
| `repeat_days_json` | `repeatDaysJson` |
| `suggested_routine_text` | `suggestedRoutineText` |
| `routine_text` | `routineText` |
| `image_url` | `imageUrl` |

## 5. 절대 변경 금지 영역

- 크롤링/수집 관련 백엔드는 사용자의 직접 요청 없이는 변경하지 않습니다.
- 특히 아래 패키지는 보호 영역입니다.
  - `backend/src/main/java/com/teukgeupjeonsa/backend/collector/`
  - 수집 스케줄러/오픈API 클라이언트/파서/수집 컨트롤러


## 6. designMe 구현 상태

- 상세 UI/UX 명세는 `docs/designMe.md`에 고정합니다.
- React/Vite 현재 구조에서는 Next.js App Router 대신 `react-router-dom` 라우트로 동일 URL 플로우를 구현합니다.
- API 연결 전에도 화면이 끊기지 않도록 `frontend/src/constants/mockData.js`를 fallback 데이터로 사용합니다.
- 새 라우트 목록은 `docs/designMe.md`의 구현 화면 플로우 표를 기준으로 관리합니다.
