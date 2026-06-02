# codexMe — Codex 작업 기록

## 2026-06-02

### 수행한 작업

- 사용자가 제공한 앱 UI/UX 플로우 이미지를 앞으로의 절대 디자인 기준으로 기록했습니다.
- 프론트 페이지 파일을 `frontend/src/pages/`에서 기능별 `frontend/src/features/*/` 폴더로 재배치했습니다.
- 기존에 `frontend/src/features/design/`에 모여 있던 CSS 모듈을 각 기능 폴더로 이동했습니다.
- `AppRouter.jsx`의 import 경로를 새 기능 폴더 구조로 변경했습니다.
- 공통 디자인 토큰을 `global.css`에 추가하고, 모바일 폰 프레임/상태바/카드/하단 내비게이션 스타일을 기준 이미지에 더 가깝게 정리했습니다.
- 백엔드 크롤링/수집 관련 패키지는 수정하지 않았습니다.

### 검증

- `frontend`에서 `npm run build`를 실행했고 Vite 프로덕션 빌드가 성공했습니다.

### 다음 작업자를 위한 주의사항

- 새로운 페이지를 추가할 때는 `frontend/src/pages/`를 다시 만들지 말고, 반드시 기능별 `frontend/src/features/<feature>/` 아래에 JSX와 CSS 모듈을 같이 둡니다.
- 변수명은 `docs/developMe.md`의 DB ↔ Java/JSON 매핑을 먼저 확인합니다.
- 백엔드 수정이 필요해도 `collector` 패키지는 직접 요청이 없는 한 건드리지 않습니다.

## 2026-06-02 추가 작업

### 수행한 작업

- `docs/designMe.md`를 추가해 사용자가 제공한 23개 화면 명세, 라우트, 구현 파일, 디자인 시스템을 프로젝트 문서로 고정했습니다.
- 홈/식단/운동/커뮤니티/마이페이지 핵심 화면을 mockData fallback 기반으로 보강해 API 서버가 없어도 모바일 앱 플로우가 자연스럽게 동작하도록 했습니다.
- `/unit/*`, `/guide`, `/diet/add`, `/exercise/*`, `/community/popular`, `/community/unit`, `/mypage/posts`, `/mypage/goal`, `/mypage/notifications`, `/mypage/data` 라우트를 추가했습니다.
- 디자인 명세의 공통 색상 변수(`--color-*`)를 전역 CSS에 반영하고 기존 `--tg-*` 토큰과 연결했습니다.
- 백엔드 크롤링/수집 관련 패키지는 수정하지 않았습니다.
