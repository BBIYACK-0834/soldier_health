# 특급전사 (Teukgeupjeonsa)

군인을 위한 맞춤형 운동/영양 관리 앱 MVP 프로젝트입니다.

## 1) 전체 아키텍처

- **Frontend (추후 구현)**: React + Vite (모바일 우선 웹앱/PWA)
- **Backend (이번 단계 구현)**: Spring Boot 3 + Java 17 + JPA + JWT
- **DB**: MySQL
- **원칙**:
  - 공공데이터포털은 **백엔드 수집기**만 접근
  - 클라이언트는 백엔드 API만 호출
  - 추천은 AI 대신 규칙 기반 MVP

## 2) 폴더 구조

```text
.
├─ backend/
│  ├─ build.gradle
│  ├─ settings.gradle
│  └─ src/main/
│     ├─ java/com/teukgeupjeonsa/backend/
│     │  ├─ TeukgeupjeonsaBackendApplication.java
│     │  ├─ auth/
│     │  ├─ common/
│     │  │  ├─ config/
│     │  │  ├─ exception/
│     │  │  ├─ response/
│     │  │  └─ security/
│     │  ├─ equipment/
│     │  ├─ seed/
│     │  ├─ unit/
│     │  └─ user/
│     └─ resources/application.yml
└─ README.md
```

## 3) 백엔드/프론트엔드 책임 분리

### Backend 책임
- JWT 인증/인가
- 사용자 프로필, 부대, 기구 설정 저장
- 식단/영양/운동 추천 API 제공(다음 단계)
- 샘플 seed 및 개발 편의 API 제공

### Frontend 책임 (다음 단계)
- 로그인/온보딩/홈/설정 UI
- 오늘 식단/운동/부족 영양소 카드 노출
- 사용자 입력 폼과 API 연동

## 4) MVP 우선순위 (현재 반영)

### 1단계 (이번 커밋)
- [x] 프로젝트 구조 생성
- [x] 인증 (회원가입/로그인/JWT, /auth/me)
- [x] 사용자 프로필 API
- [x] 부대 설정 API
- [x] 기구 설정 API
- [x] 샘플 seed API

### 다음 단계
- 식단 조회/영양 계산/운동 추천 엔진
- 홈/운동/보충 페이지
- 알람 CRUD

## 5) DB 스키마 개요 (현재 구현 범위)

- users
- military_units
- user_unit_settings
- equipments
- user_equipments

> 나머지 meal/workout/nutrition 관련 테이블은 2~3단계에서 확장 예정.

## 6) 주요 API 목록 (현재 구현)

### 인증
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### 사용자 프로필
- `GET /api/users/me`
- `PUT /api/users/me/profile`
- `PUT /api/users/me/goals`
- `PUT /api/users/me/workout-preferences`

### 부대
- `GET /api/units`
- `GET /api/units/search?keyword=`
- `POST /api/users/me/unit`
- `GET /api/users/me/unit`

### 기구
- `GET /api/equipments`
- `POST /api/users/me/equipments`
- `GET /api/users/me/equipments`

### 개발용 Seed
- `POST /api/dev/seed/sample-data`

## 7) 실행 방법

### 사전 준비
- Java 17
- MySQL 8+

### 환경변수 예시

```bash
export DB_URL='jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='root'
export DB_PASSWORD='root'
export JWT_SECRET='change-this-secret-key-change-this-secret-key'
export JWT_ACCESS_TOKEN_VALIDITY_SECONDS='86400'
```

### 실행

```bash
cd backend
./gradlew bootRun
```

## 8) Docker MySQL 예시

```bash
docker run -d \
  --name tg-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=teukgeupjeonsa \
  -p 3306:3306 \
  mysql:8.4
```

## 9) 빠른 시작 시나리오

1. 서버 실행
2. `POST /api/dev/seed/sample-data` 호출
3. `POST /api/auth/signup`으로 가입
4. 토큰으로 `GET /api/users/me` 호출
5. `POST /api/users/me/unit`, `POST /api/users/me/equipments` 설정

## 10) React Native/Expo 확장 포인트

- 알람 스키마는 `hour/minute/repeat_days_json` 중심으로 저장 예정
- 웹에서는 CRUD + 다음 알람 계산, 모바일에서는 로컬 알림 스케줄러 연결
- API 응답은 앱/웹 공용 사용 가능하게 유지

---

## 11) Frontend (현재 추가된 MVP 기본 골격)

### 폴더 구조

```text
frontend/
├─ index.html
├─ package.json
├─ vite.config.js
└─ src/
   ├─ main.jsx
   ├─ api/
   │  ├─ httpClient.js
   │  ├─ authApi.js
   │  ├─ userApi.js
   │  ├─ unitApi.js
   │  └─ equipmentApi.js
   ├─ app/
   │  ├─ AppContext.jsx
   │  └─ AppRouter.jsx
   ├─ components/layout/
   │  ├─ MobileShell.jsx
   │  └─ MobileShell.module.css
   ├─ features/
   │  ├─ auth/AuthForm.module.css
   │  ├─ onboarding/OnboardingForm.module.css
   │  └─ home/HomeCards.module.css
   ├─ pages/
   │  ├─ LoginPage.jsx
   │  ├─ SignupPage.jsx
   │  ├─ OnboardingPage.jsx
   │  └─ HomePage.jsx
   └─ styles/global.css
```

### 프론트 실행

```bash
cd frontend
npm install
npm run dev
```

### 프론트 환경변수 예시

```bash
# frontend/.env
VITE_API_BASE_URL=http://localhost:8080
```

---

## 12) 2단계 추가 구현(식단/영양/운동/알람)

### Backend 추가 API
- 식단: `GET /api/meals/today`, `GET /api/meals?date=`, `GET /api/meals/week?startDate=`
- 영양: `GET /api/nutrition/today`, `GET /api/nutrition/recommendation/today`
- PX/보유식품: `GET /api/px-products`, `GET/POST/PUT/DELETE /api/users/me/owned-foods`
- 운동: `GET /api/workouts/recommendation/today`, `GET /api/workouts/plan`
- 알람: `GET /api/alarms/me`, `POST /api/alarms`, `PUT /api/alarms/{id}`, `DELETE /api/alarms/{id}`
- 개발용 시드: `POST /api/dev/seed/sample-meals`

### Frontend 홈 연동
- 홈에서 오늘 식단/운동/부족 영양소/다음 알람을 API로 조회하여 표시
