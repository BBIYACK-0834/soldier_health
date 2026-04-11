# 특급전사 (Teukgeupjeonsa)

군인을 위한 운동/영양 관리 앱 MVP입니다. 현재 문서는 **Codespaces에서 backend를 즉시 실행하고 회원가입/로그인/JWT 확인**하는 데 집중합니다.

## 1. Backend 빠른 실행 (Codespaces 기준)

### 1) 요구사항
- Java 17
- Docker (MySQL 실행용)

Java 확인:

```bash
java -version
```

출력에 `17`이 포함되어야 합니다.

---

### 2) MySQL 실행

```bash
docker run -d \
  --name tg-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=teukgeupjeonsa \
  -p 3306:3306 \
  mysql:8.4
```

컨테이너 상태 확인:

```bash
docker ps
```

---

### 3) backend 환경변수 설정

#### 방법 A: `.env.example` 복사

```bash
cd backend
cp .env.example .env
```

`.env` 예시 값:

```env
DB_URL=jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-this-secret-key-change-this-secret-key
JWT_ACCESS_TOKEN_VALIDITY_SECONDS=86400
SERVER_PORT=8080
```

> 참고: Spring Boot는 기본적으로 `.env`를 자동 로드하지 않습니다. Codespaces에서는 아래 export 방식이 가장 확실합니다.

#### 방법 B: export 방식 (권장)

```bash
export DB_URL='jdbc:mysql://localhost:3306/teukgeupjeonsa?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='root'
export DB_PASSWORD='root'
export JWT_SECRET='change-this-secret-key-change-this-secret-key'
export JWT_ACCESS_TOKEN_VALIDITY_SECONDS='86400'
export SERVER_PORT='8080'
```

---

### 4) backend 실행 (Gradle Wrapper 사용)

이 저장소는 PR 시스템의 바이너리 파일 제한 때문에 `gradle-wrapper.jar`를 커밋하지 않습니다.
대신 아래 한 번만 실행해서 wrapper 파일을 생성하세요.

```bash
cd backend
./scripts/bootstrap-wrapper.sh
```

생성 후 실행:

```bash
cd backend
./gradlew bootRun
```

Wrapper 관련 파일:

- `backend/gradlew` (커밋됨)
- `backend/gradlew.bat` (커밋됨)
- `backend/gradle/wrapper/gradle-wrapper.properties` (커밋됨)
- `backend/gradle/wrapper/gradle-wrapper.jar` (로컬 생성 파일, 커밋 제외)

---

### 5) seed 데이터 입력

서버 실행 후:

```bash
curl -X POST http://localhost:8080/api/dev/seed/sample-data
```

---

### 6) 인증 API 테스트

#### 회원가입

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "email":"test@example.com",
    "password":"test1234",
    "nickname":"테스트",
    "goalType":"BULK",
    "workoutLevel":"BEGINNER",
    "branchType":"ARMY"
  }'
```

#### 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email":"test@example.com",
    "password":"test1234"
  }'
```

응답의 `data.accessToken` 값을 복사합니다.

#### 내 정보 조회

```bash
curl http://localhost:8080/api/auth/me \
  -H 'Authorization: Bearer <ACCESS_TOKEN>'
```

---

## 2. CORS/JWT 동작 기준

- 허용 Origin: `http://localhost:5173`
- 허용 Method: `GET, POST, PUT, DELETE, OPTIONS`
- `OPTIONS /**` preflight 허용
- `/api/auth/signup`, `/api/auth/login`은 인증 없이 접근
- `/api/auth/me`는 Bearer 토큰 필요
- JWT는 `Authorization: Bearer <token>` 헤더로 전달

---

## 3. 자주 발생하는 오류 해결

### CORS 에러
- 프론트 주소가 `http://localhost:5173`인지 확인
- 백엔드 재시작 후 preflight(`OPTIONS`)가 200/204로 처리되는지 확인

### DB 연결 실패
- `docker ps`로 MySQL 컨테이너 상태 확인
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 값 확인

### 포트 충돌
- 8080 사용 중이면 `SERVER_PORT`를 다른 포트로 변경
- 3306 충돌 시 Docker 포트 매핑 변경

### Java 버전 문제
- `java -version`이 17이 아니면 Java 17로 전환

---

## 4. 프론트엔드 실행 (다음 단계)

프론트엔드 `.env.example`, axios/JWT 흐름 정리는 다음 작업에서 반영합니다.
