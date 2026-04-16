# 특급전사 (soldier_health)

군인을 위한 운동/영양 관리 앱입니다.

## 1) 실행 요구사항
- Java 17
- Node.js 20+
- Docker (MySQL)

```bash
java -version
node -v
docker -v
```

## 2) MySQL 실행
```bash
docker run -d \
  --name tg-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=teukgeupjeonsa \
  -p 3306:3306 \
  mysql:8.4
```

## 3) Backend 실행
```bash
cd backend
cp .env.example .env
./scripts/bootstrap-wrapper.sh
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

PUBLIC_MEAL_API_BASE_URL=https://openapi.mnd.go.kr
PUBLIC_MEAL_API_SERVICE_KEY=<MND_OPENAPI_SERVICE_KEY>
PUBLIC_MEAL_API_ROWS=200
PUBLIC_MEAL_API_TYPE=json

MEAL_COLLECTOR_TIMEOUT=10000
MEAL_COLLECTOR_FIXED_SERVICES=1570,5861,1691,3182,8623,7296,1862,2171,7021,9030,ATC,5397,3296,8902,2621,3389,5021,6176,3007,5322,5067,7162,1575,6335,7369,2136,1968,6685,2291,7652,7461,STANDARD
MEAL_COLLECTOR_ATC_SERVICE_CODE=DS_TB_MNDT_DATEBYMLSVC_ATC
MEAL_COLLECTOR_STANDARD_SERVICE_CODE=DS_TB_MNDT_DATEBYMLSVC_STANDARD
```

## 4) Frontend 실행
```bash
cd frontend
cp .env.example .env
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

`frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

## 5) 개발용 seed API
```bash
curl -X POST http://localhost:8080/api/dev/seed/sample-data
curl -X POST http://localhost:8080/api/dev/seed/sample-meals
```

## 6) 국방부 OpenAPI 식단 수집 (고정 서비스 코드 방식)

### 수집 구조
1. `meal-collector.fixed-services`의 고정 서비스 목록을 순회
2. 각 서비스에 대해 `https://openapi.mnd.go.kr/{KEY}/{TYPE}/{SERVICE}/1/{ROWS}` 호출
3. 응답 JSON 파싱/정규화
4. `meal_menus(service_code, meal_date)` 기준 upsert

### 서비스 코드 규칙
- 숫자 코드(예: `3389`)는 `DS_TB_MNDT_DATEBYMLSVC_3389`로 자동 변환
- `ATC`, `STANDARD`는 별칭이며 아래 설정값으로 실제 서비스 코드로 치환
  - `meal-collector.atc-service-code`
  - `meal-collector.standard-service-code`

### 조회 구조와 연결
- 사용자 식단 조회는 `user -> primary unit -> unit.dataSourceKey -> meal_menus.serviceCode` 경로를 사용
- 따라서 각 부대의 `dataSourceKey`는 OpenAPI 실제 서비스 코드와 일치해야 함

### 관리자 수동 실행 API
```bash
# 전체 수집: 고정 서비스 목록 전체 순회
curl -X POST http://localhost:8080/api/admin/collect/meals/openapi \
  -H "Authorization: Bearer <TOKEN>"

# 서비스 코드 기준 단건 수집
# (숫자/별칭/전체 서비스 코드 모두 허용)
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/3389" \
  -H "Authorization: Bearer <TOKEN>"
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/ATC" \
  -H "Authorization: Bearer <TOKEN>"
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/DS_TB_MNDT_DATEBYMLSVC_3389" \
  -H "Authorization: Bearer <TOKEN>"
```

### 관련 설정(application.yml)
- `public-meal.api.base-url`
- `public-meal.api.service-key`
- `public-meal.api.rows`
- `public-meal.api.type`
- `meal-collector.timeout-millis`
- `meal-collector.fixed-services`
- `meal-collector.atc-service-code`
- `meal-collector.standard-service-code`

## 7) 인증/CORS
- `Authorization: Bearer <token>`
- `OPTIONS /**` 허용
- 허용 Origin
  - `http://localhost:*`
  - `http://127.0.0.1:*`
  - `https://localhost:*`
  - `https://*.app.github.dev`

## 8) 참고
- `backend/gradle/wrapper/gradle-wrapper.jar`는 저장소에 포함되지 않습니다.
- 네트워크 제한 환경에서는 Gradle plugin/배포본 다운로드가 실패할 수 있습니다.
