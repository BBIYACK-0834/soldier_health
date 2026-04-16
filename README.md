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
MEAL_COLLECTOR_OPENAPI_LIST_URL_TEMPLATE=https://www.data.mil.kr/openapi/list.do?apiType=OPEN_API&search=%EC%8B%9D%EB%8B%A8&page={page}
MEAL_COLLECTOR_MAX_PAGES=4
MEAL_COLLECTOR_TIMEOUT=10000
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

## 6) 국방부 OpenAPI 식단 수집(신규 파이프라인)

### 수집 구조
1. OpenAPI 목록 페이지에서 `식단` 관련 항목 수집
2. 상세 페이지에서 `SERVICE`, `OPENAPI URL`, 부대명 추출
3. `https://openapi.mnd.go.kr/{KEY}/{TYPE}/{SERVICE}/{START}/{END}` 형식으로 API 호출
4. 응답 정규화 후 `meal_menus`에 upsert
5. `unit_api_sources`에 부대↔서비스 매핑 저장

### 관리자 수동 실행 API
```bash
# 전체 수집: 목록 크롤링 + 상세 파싱 + OpenAPI 호출 + DB 저장
curl -X POST http://localhost:8080/api/admin/collect/meals/openapi \
  -H "Authorization: Bearer <TOKEN>"

# 부대명 기준 단건 수집 (unit_api_sources 기준)
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/%EC%A0%9C3389%EB%B6%80%EB%8C%80" \
  -H "Authorization: Bearer <TOKEN>"

# serviceName 기준 단건 수집 (unit_api_sources 기준)
curl -X POST "http://localhost:8080/api/admin/collect/meals/openapi/service/DS_TB_MNDT_DATEBYMLSVC_3389" \
  -H "Authorization: Bearer <TOKEN>"
```

### 관련 설정(application.yml)
- `public-meal.api.base-url`
- `public-meal.api.service-key`
- `public-meal.api.rows`
- `public-meal.api.type`
- `meal-collector.openapi-list-url-template`
- `meal-collector.max-pages`
- `meal-collector.timeout-millis`

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
