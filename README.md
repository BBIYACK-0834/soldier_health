# 특급전사 (Soldier Health)

특급전사는 군 복무 중인 장병을 위한 **군 특화 운동·식단 관리 플랫폼**입니다. 일반 헬스 앱이 다루기 어려운 군 급식, PX 상품, 부대 운동기구 정보를 활용해 장병이 가장 쉽게 운동하고 식단을 관리할 수 있도록 돕는 것을 목표로 합니다.

## 1. 기획의도

### 프로젝트 배경

기존 운동 앱은 대부분 일반인을 대상으로 설계되어 있어 군대라는 특수한 환경을 충분히 반영하지 못합니다. 실제 군 생활에서는 체력 향상, 다이어트, 벌크업, 특급전사 선발 등을 목표로 운동을 시작하는 장병이 많지만, 운동 경험이 없는 초보자가 많고 올바른 식단 관리 방법이나 칼로리 계산 방법을 알기 어렵습니다.

군대에서는 다음과 같은 환경적 제약이 있습니다.

- 모든 장병이 동일한 급식을 제공받습니다.
- 식사 메뉴가 사전에 정해져 있습니다.
- 음식 구매처가 대부분 PX로 제한됩니다.
- 개인이 식단을 자유롭게 구성하기 어렵습니다.
- 운동은 하지만 체계적인 운동·영양 정보는 부족합니다.

반대로, 군대는 급식과 구매처가 표준화되어 있어 관련 데이터를 잘 활용하면 일반 운동 앱보다 더 현실적이고 정확한 식단 관리가 가능하다는 장점도 있습니다.

### 문제 인식

#### 운동 방법에 대한 정보 부족

운동을 처음 시작하는 장병은 어떤 운동을 해야 하는지, 어떤 순서로 해야 하는지, 세트 수와 반복 횟수는 어떻게 정해야 하는지, 운동기구를 어떻게 사용해야 하는지 알기 어렵습니다. 결국 주변 사람에게 계속 질문하거나 인터넷 정보를 무작정 따라 하게 됩니다.

#### 식단 관리의 어려움

다이어트와 벌크업 모두 식단이 중요하지만 군대에서는 급식 칼로리, 단백질 섭취량, 벌크업·감량에 필요한 섭취량, PX 음식의 영양 정보를 한 번에 확인하기 어렵습니다. 이 때문에 운동보다 식단 관리가 더 어렵게 느껴지는 문제가 있습니다.

### 해결 방향

특급전사는 군대 환경에 최적화된 운동·식단 관리 서비스를 제공합니다.

- **군 급식 기반 식단 관리**: 국방부 급식 데이터를 활용해 식단, 칼로리, 탄수화물·단백질·지방 정보를 확인합니다.
- **PX 기반 추가 식단 관리**: PX 상품 영양 정보를 기반으로 단백질 식품, 벌크업용 식품, 다이어트용 식품을 추천합니다.
- **운동 추천 시스템**: 운동 경험과 목표에 따라 입문자 루틴, 체력단련 루틴, 특급전사 준비 루틴, 벌크업 루틴, 다이어트 루틴을 제공합니다.
- **부대 헬스장 데이터 활용**: 부대별 보유 운동기구를 바탕으로 가능한 운동과 대체 운동을 추천합니다.

### 기대 효과와 최종 비전

특급전사는 운동 초보자의 진입장벽을 낮추고, 군 급식·PX 데이터를 기반으로 현실적인 식단 관리를 지원하며, 체중 감량·체중 증가·체력 향상 등 개인 목표 달성을 도와 장병의 건강한 군 생활에 기여합니다.

최종적으로는 단순한 운동 기록 앱이 아니라, **“군 장병이 가장 쉽게 운동하고, 가장 쉽게 식단을 관리할 수 있는 군 특화 헬스 플랫폼”**을 지향합니다.

## 2. 기술 스택

### Frontend

- React 18
- Vite 6
- React Router DOM 6
- Axios
- CSS Modules
- ESLint 9

### Backend

- Java 17
- Spring Boot 3.3.4
- Spring Web / WebFlux
- Spring Data JPA
- Spring Security
- Spring Validation
- JWT (`jjwt`)
- Lombok
- Jsoup

### Database / Infrastructure

- MySQL 8.4
- Docker
- 국방부 OpenAPI 식단 데이터 연동

## 3. 사용방법

### 3.1 실행 요구사항

- Java 17
- Node.js 20+
- Docker 또는 로컬 MySQL 8.x

```bash
java -version
node -v
docker -v
```

### 3.2 MySQL 실행

```bash
docker run -d \
  --name tg-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=teukgeupjeonsa \
  -p 3306:3306 \
  mysql:8.4
```

컨테이너 생성 이후에는 아래 명령으로 상태를 확인하거나 다시 시작할 수 있습니다.

```bash
docker ps
docker start tg-mysql
```

### 3.3 Backend 실행

```bash
cd backend
set -a; source .env; set +a
./gradlew bootRun
```

`backend/.env` 예시:

```env
DB_URL=jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-this-secret-key-change-this-secret-key
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=86400
SERVER_PORT=8080
# 로그인 403(CORS) 방지를 위해 프론트 배포/포트 포워딩 Origin을 쉼표로 추가하세요.
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,https://localhost:*,https://*.app.github.dev,https://*.github.dev,https://*.vercel.app,https://*.netlify.app

PUBLIC_MEAL_API_BASE_URL=https://openapi.mnd.go.kr
PUBLIC_MEAL_API_SERVICE_KEY=<MND_OPENAPI_SERVICE_KEY>
PUBLIC_MEAL_API_ROWS=200
PUBLIC_MEAL_API_TYPE=json

MEAL_COLLECTOR_TIMEOUT=10000
MEAL_COLLECTOR_FIXED_SERVICES=1570,5861,1691,3182,8623,7296,1862,2171,7021,9030,ATC,5397,3296,8902,2621,3389,5021,6176,3007,5322,5067,7162,1575,6335,7369,2136,1968,6685,2291,7652,7461,STANDARD
MEAL_COLLECTOR_ATC_SERVICE_CODE=DS_TB_MNDT_DATEBYMLSVC_ATC
MEAL_COLLECTOR_STANDARD_SERVICE_CODE=DS_TB_MNDT_DATEBYMLSVC_STANDARD
```

> 저장소에는 `backend/.env.example`도 포함되어 있으므로 필요한 경우 예시 파일을 복사해 사용할 수 있습니다.

### 3.4 Frontend 실행

```bash
cd frontend
cp .env.example .env
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

`frontend/.env` 예시:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3.5 개발용 seed API

```bash
curl -X POST http://localhost:8080/api/dev/seed/sample-data
curl -X POST http://localhost:8080/api/dev/seed/sample-meals
```

### 3.6 국방부 OpenAPI 식단 수집

#### 수집 구조

1. `meal-collector.fixed-services`의 고정 서비스 목록을 순회합니다.
2. 각 서비스에 대해 `https://openapi.mnd.go.kr/{KEY}/{TYPE}/{SERVICE}/1/{ROWS}`를 호출합니다.
3. 응답 JSON을 파싱하고 정규화합니다.
4. `meal_menus(service_code, meal_date)` 기준으로 upsert합니다.

#### 서비스 코드 규칙

- 숫자 코드(예: `3389`)는 `DS_TB_MNDT_DATEBYMLSVC_3389`로 자동 변환됩니다.
- `ATC`, `STANDARD`는 별칭이며 아래 설정값으로 실제 서비스 코드로 치환됩니다.
  - `meal-collector.atc-service-code`
  - `meal-collector.standard-service-code`

#### 조회 구조와 연결

사용자 식단 조회는 `user -> primary unit -> unit.dataSourceKey -> meal_menus.serviceCode` 경로를 사용합니다. 따라서 각 부대의 `dataSourceKey`는 OpenAPI 실제 서비스 코드와 일치해야 합니다.

#### 관리자 수동 실행 API

```bash
# 전체 수집: 고정 서비스 목록 전체 순회
curl -X POST http://localhost:8080/api/admin/collect/meals/openapi \
  -H "Authorization: Bearer <TOKEN>"

# 서비스 코드 기준 단건 수집
# 숫자/별칭/전체 서비스 코드 모두 허용
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/7296" \
  -H "Authorization: Bearer <TOKEN>"
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/ATC" \
  -H "Authorization: Bearer <TOKEN>"
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/DS_TB_MNDT_DATEBYMLSVC_3389" \
  -H "Authorization: Bearer <TOKEN>"
```

#### 관련 설정 (`application.yml`)

- `public-meal.api.base-url`
- `public-meal.api.service-key`
- `public-meal.api.rows`
- `public-meal.api.type`
- `meal-collector.timeout-millis`
- `meal-collector.fixed-services`
- `meal-collector.atc-service-code`
- `meal-collector.standard-service-code`


### 3.7 인증/CORS

- 인증 헤더: `Authorization: Bearer <token>`
- `OPTIONS /**` 허용
- 기본 허용 Origin
  - `http://localhost:*`
  - `http://127.0.0.1:*`
  - `https://localhost:*`
  - `https://*.app.github.dev`
  - `https://*.github.dev`
  - `https://*.vercel.app`
  - `https://*.netlify.app`

### 3.8 정제 식품 영양성분 xlsx 수동 import

정제된 `food_data/food_data_DB.xlsx` 파일은 서버 시작 시 자동 적재하지 않고, 필요할 때 Gradle 태스크로 수동 실행합니다. import 대상은 `food_master` 시트와 `food_alias` 시트이며, `review_needed`, `정제기준` 시트는 DB에 적재하지 않습니다.

#### import 전에 확인할 것

1. MySQL이 실행 중이어야 합니다.
2. `backend/.env`의 DB 접속 정보가 실제 DB와 맞아야 합니다.
3. 정제 xlsx 파일이 저장소 루트 기준 `food_data/food_data_DB.xlsx`에 있어야 합니다.
4. 현재 import는 재실행 가능하도록 기존 `food_aliases`와 `foods` 데이터를 지운 뒤 xlsx 기준으로 다시 넣습니다. 운영 DB에서 실행하기 전에는 백업 여부를 먼저 확인하세요.

#### 기본 실행 방법

저장소 루트에서 백엔드 폴더로 이동한 뒤 `.env`를 로드하고 import 태스크를 실행합니다.

```bash
cd backend
set -a; source .env; set +a
./gradlew importFoods
```

`importFoods`의 기본 파일 경로는 백엔드 폴더 기준 `../food_data/food_data_DB.xlsx`입니다. 즉 저장소 전체 경로가 `/workspaces/soldier_health`라면 기본으로 `/workspaces/soldier_health/food_data/food_data_DB.xlsx` 파일을 읽습니다.

#### 파일 경로를 직접 지정해서 실행하기

Codespaces나 로컬 환경에서 절대 경로를 명확히 지정하고 싶다면 `-PfoodFile`을 사용합니다.

```bash
cd backend
set -a; source .env; set +a
./gradlew importFoods -PfoodFile="/workspaces/soldier_health/food_data/food_data_DB.xlsx"
```

현재 컨테이너처럼 저장소가 `/workspace/soldier_health`에 있는 환경에서는 아래처럼 지정할 수도 있습니다.

```bash
cd backend
set -a; source .env; set +a
./gradlew importFoods -PfoodFile="/workspace/soldier_health/food_data/food_data_DB.xlsx"
```

#### import 처리 방식

- `food_master` 시트의 `food_name`, `display_category`, `basis`, 영양성분 컬럼을 `foods` 테이블에 저장합니다.
- `food_alias` 시트의 `raw_food_name`, `maker_name`, `final_food_name`, `display_category`를 `food_aliases` 테이블에 저장합니다.
- `final_food_name`이 비어 있으면 `raw_food_name`과 같은 대표 음식명으로 연결을 시도합니다.
- 연결할 대표 음식이 `foods`에 없으면 해당 alias는 건너뛰고, 건너뛴 개수를 콘솔에 출력합니다.
- 숫자 영양성분은 `Double`/`Integer`로 변환하며 빈 문자열, `-`, 빈 셀은 `0`이 아니라 `null`로 저장합니다.
- 검색 품질을 위해 음식명 공백을 제거한 `searchName`도 함께 저장합니다. 예: `김치 찌개` → `김치찌개`.

#### 완료 후 콘솔 출력 예시

```text
식품 데이터 import 완료
foods 삽입 개수: 1234
food_aliases 삽입 개수: 5678
매핑 실패로 건너뛴 alias 개수: 90
```


## 4. 기능 설명

### 회원가입·로그인·온보딩

- 이메일 기반 회원가입과 로그인을 제공합니다.
- 회원가입 시 프로필 이미지를 등록할 수 있습니다.
- 최초 진입 시 군 생활 정보, 부대, 보유 운동기구 등 개인화 추천에 필요한 기본 정보를 설정합니다.

### 홈 대시보드

- 오늘의 운동, 식단, 목표 진행률 등 핵심 정보를 한 화면에서 확인할 수 있는 모바일 중심 대시보드를 제공합니다.
- 사용자 목표와 군 생활 정보에 맞춘 요약 정보를 제공합니다.

### 군 급식 기반 식단 관리

- 국방부 OpenAPI로 수집한 부대 급식 데이터를 기반으로 식단을 조회합니다.
- 급식 칼로리와 탄수화물·단백질·지방 정보를 확인합니다.
- 하루 총 섭취량과 목표 대비 섭취 상태를 분석하는 기능으로 확장할 수 있습니다.

### PX 기반 추가 식단 관리

- PX 상품 정보를 바탕으로 추가 섭취 식품을 기록하고 관리합니다.
- 단백질 보충, 벌크업, 다이어트 등 목표별 식품 추천 기능을 제공합니다.
- 급식과 PX 섭취를 통합해 현실적인 군 생활 식단 관리를 지원합니다.

### 운동 추천·운동 기록

- 운동 입문자, 체력단련, 특급전사 준비, 벌크업, 다이어트 등 목표별 루틴을 제공합니다.
- 세트 수, 반복 횟수, 운동 수행 여부를 기록합니다.
- 운동 세션과 루틴 편집 기능을 통해 개인 운동 계획을 관리합니다.

### 부대 운동기구 기반 추천

- 사용자가 설정한 부대의 운동기구 정보를 확인합니다.
- 보유 장비로 수행 가능한 운동을 추천합니다.
- 장비가 부족한 경우 대체 운동 추천으로 운동 공백을 줄입니다.

### 커뮤니티·가이드·마이페이지

- 커뮤니티를 통해 운동·식단 관련 게시글을 확인하고 소통할 수 있습니다.
- 운동 가이드 화면에서 초보자도 따라 하기 쉬운 정보를 제공합니다.
- 마이페이지에서 프로필, 목표, 알림, 데이터 관리 설정을 변경할 수 있습니다.

## 5. 개발 참고

- `backend/gradle/wrapper/gradle-wrapper.jar`는 저장소에 포함되지 않습니다.
- 네트워크 제한 환경에서는 Gradle plugin 또는 배포본 다운로드가 실패할 수 있습니다.
- UI/UX 기준 원문과 구현 라우트는 [`docs/designMe.md`](docs/designMe.md)에 정리합니다.
- 라우트/API 요약과 DB ↔ Java/JSON 변수명 매핑은 [`docs/developMe.md`](docs/developMe.md)에 정리합니다.
- Codex가 수행한 구조 변경과 검증 기록은 [`docs/codexMe.md`](docs/codexMe.md)에 누적합니다.
- 프론트 페이지는 기능별로 `frontend/src/features/<feature>/` 아래에 JSX와 CSS 모듈을 함께 둡니다.
- 크롤링/수집 관련 백엔드(`backend/src/main/java/com/teukgeupjeonsa/backend/collector/`)는 사용자의 직접 요청 없이는 수정하지 않습니다.
