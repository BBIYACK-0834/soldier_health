# 특급전사 (soldier_health)

군인을 위한 운동/영양 관리 앱입니다.  
이 문서는 **GitHub Codespaces 기준으로 회원가입 → 로그인 → 초기 설정 → 홈 화면 흐름을 바로 확인**할 수 있게 정리했습니다.

---

## 1) 필수 요구사항

- Java 17
- Node.js 20+
- Docker (MySQL용)

확인 명령어:

```bash
java -version
node -v
docker -v
```

---

## 2) MySQL 실행 (Docker)

```bash
docker run -d \
  --name tg-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=teukgeupjeonsa \
  -p 3306:3306 \
  mysql:8.4
```

상태 확인:

```bash
docker ps
```

---

## 3) Backend 설정 및 실행

### 3-1. 환경변수 파일 준비

```bash
cd backend
cp .env.example .env
```

`backend/.env.example`

```env
DB_URL=jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-this-secret-key-change-this-secret-key
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=86400
SERVER_PORT=8080
```

> 참고: `.env` 자동 로딩 대신, 필요 시 아래처럼 export 방식 사용 가능

```bash
export DB_URL='jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='root'
export DB_PASSWORD='root'
export JWT_SECRET='change-this-secret-key-change-this-secret-key'
export JWT_ACCESS_TOKEN_VALIDITY_SECONDS='86400'
export SERVER_PORT='8080'
```

### 3-2. Backend 실행

처음 실행에서 `gradle-wrapper.jar`가 없으면 아래를 1회 실행하세요.

```bash
cd backend
./scripts/bootstrap-wrapper.sh
```

그 다음 실행:

```bash
cd backend
./gradlew bootRun
```

---

## 4) Frontend 설정 및 실행

### 4-1. 환경변수 파일 준비

```bash
cd frontend
cp .env.example .env
```

`frontend/.env.example`

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 4-2. Frontend 실행

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

---

## 5) Codespaces 포트 URL 사용법

Codespaces에서는 `localhost` 대신 공개 URL이 필요할 수 있습니다.

예시:

```env
VITE_API_BASE_URL=https://<codespace-name>-8080.app.github.dev
```

- 프론트: `https://<codespace-name>-5173.app.github.dev`
- 백엔드: `https://<codespace-name>-8080.app.github.dev`
- Ports 탭에서 5173/8080 포트를 열고 접근 권한(Private/Public)을 설정하세요.

---

## 6) Seed 데이터 넣기

백엔드 실행 후 아래 중 하나 실행:

```bash
curl -X POST http://localhost:8080/api/dev/seed/sample-data
```

또는 식단만 다시 넣기:

```bash
curl -X POST http://localhost:8080/api/dev/seed/sample-meals
```

기본 시드에는 부대(6개+), 기구, 샘플 식단, 영양 데이터, PX 데이터가 포함됩니다.

공공데이터 API에서 식단 수집(권장):

```bash
# backend/.env 에 API 정보 설정 후
curl -X POST "http://localhost:8080/api/dev/seed/public-meals?startDate=2026-01-01&endDate=2026-12-31"

# 특정 부대명 키워드로만 수집
curl -X POST "http://localhost:8080/api/dev/seed/public-meals?startDate=2026-01-01&endDate=2026-12-31&unitKeyword=%EC%9C%A1%EA%B5%B0"
```

환경변수(`backend/.env`):

```env
PUBLIC_MEAL_API_BASE_URL=https://<공공데이터-식단-api-endpoint>
PUBLIC_MEAL_API_SERVICE_KEY=<발급받은-서비스키>
PUBLIC_MEAL_API_ROWS=200
```

---

## 7) 기능 테스트 순서 (권장)

1. `/signup`에서 회원가입 (이메일/비밀번호/닉네임)
2. `/login`에서 로그인
3. 최초 로그인 시 `/onboarding`으로 이동
4. 초기 설정 입력 후 저장
   - 키/몸무게
   - 목표/운동수준
   - 주당 운동 횟수/운동 시간
   - 운동 가능 요일
   - 부대 선택
   - 보유 기구 선택
5. 홈(`/`)에서 식단/운동추천/영양정보 확인

---

## 8) 인증/CORS 동작 기준

- `Authorization: Bearer <token>` 방식 사용
- `OPTIONS /**` preflight 허용
- 허용 Origin 패턴:
  - `http://localhost:*`
  - `http://127.0.0.1:*`
  - `https://localhost:*`
  - `https://*.app.github.dev`
- 허용 Method: `GET, POST, PUT, DELETE, PATCH, OPTIONS`

---

## 9) 흔한 오류 해결

### (1) CORS 오류
- 프론트의 `VITE_API_BASE_URL`이 실제 백엔드 주소와 동일한지 확인
- 백엔드 재시작 후 브라우저 새로고침

### (2) 부대 목록이 안 뜰 때
- `/api/dev/seed/sample-data` 재실행
- `/api/units` 응답이 비어있는지 확인

### (3) 홈에서 식단이 비어 있을 때
- 데이터가 없으면 정상적으로 빈 상태 문구가 표시됨
- 샘플 식단 필요 시 `/api/dev/seed/sample-meals` 실행

### (4) Java 버전 문제
- `java -version`이 17인지 확인

### (5) 포트 충돌
- 8080/5173/3306 충돌 시 포트를 변경하고 `.env` 값도 함께 수정

---

## 10) 참고

- 백엔드 Gradle Wrapper 스크립트 포함: `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/gradle-wrapper.properties`
- 바이너리(wrapper jar)는 저장소에 포함하지 않았습니다. 네트워크 제한이 있으면 Gradle 배포본 다운로드가 실패할 수 있습니다.
