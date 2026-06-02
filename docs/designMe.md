# designMe.md — 군인 맞춤 다이어트 & 운동 관리 앱 UI/UX 기능 및 디자인 명세

## 서비스 개요

군 장병을 대상으로 한 식단, 운동, 체중, 커뮤니티 기반 건강 관리 웹/앱 서비스입니다.

핵심 목표:

- 군 부대 기준 식단 기록
- 개인 목표 체중 및 칼로리 관리
- 운동 루틴 기록 및 운동 데이터 불러오기
- 부대 기반 커뮤니티 제공
- 개인 건강 데이터 관리
- 오프라인 환경에서도 사용 가능한 구조 고려

## 구현 화면 플로우

| 번호 | 화면 | 구현 라우트 | 구현 파일 |
| --- | --- | --- | --- |
| 01 | 온보딩 | `/onboarding` | `frontend/src/features/onboarding/OnboardingPage.jsx` |
| 02 | 로그인 | `/login` | `frontend/src/features/auth/LoginPage.jsx` |
| 03 | 회원가입 | `/signup` | `frontend/src/features/auth/SignupPage.jsx` |
| 04~06 | 부대 찾기/검색/선택 | `/unit/setup`, `/unit/search`, `/unit/select/:unitId` | `frontend/src/features/setup/UnitSelectPage.jsx` |
| 07 | 부대 선택 완료 | `/unit/complete` | `frontend/src/features/setup/UnitCompletePage.jsx` |
| 08 | 앱 사용 가이드 | `/guide` | `frontend/src/features/guide/GuidePage.jsx` |
| 09 | 홈 대시보드 | `/home` | `frontend/src/features/home/HomePage.jsx` |
| 10 | 식단 기록 | `/diet`, `/diet/:date` | `frontend/src/features/nutrition/NutritionPage.jsx` |
| 11 | 식단 추가/음식 검색 | `/diet/add`, `/diet/search` | `frontend/src/features/nutrition/DietAddPage.jsx` |
| 12 | 운동 기록 요약 | `/exercise` | `frontend/src/features/workout/WorkoutPage.jsx` |
| 13~15 | 기구 선택/데이터셋/루틴 편집 | `/exercise/add/equipment`, `/exercise/datasets`, `/exercise/routine/edit` | `frontend/src/features/workout/WorkoutEditPage.jsx` |
| 16~18 | 커뮤니티 전체/인기/우리 부대 | `/community`, `/community/popular`, `/community/unit` | `frontend/src/features/community/CommunityPage.jsx` |
| 19 | 마이페이지 | `/mypage` | `frontend/src/features/profile/ProfilePage.jsx` |
| 20 | 내 게시글 | `/mypage/posts` | `frontend/src/features/profile/MyPostsPage.jsx` |
| 21 | 목표 설정 | `/mypage/goal` | `frontend/src/features/profile/GoalSettingsPage.jsx` |
| 22 | 알림 설정 | `/mypage/notifications` | `frontend/src/features/profile/NotificationSettingsPage.jsx` |
| 23 | 데이터 관리 | `/mypage/data` | `frontend/src/features/profile/DataManagementPage.jsx` |

## 디자인 시스템

공통 색상은 `frontend/src/styles/global.css`에 정의합니다.

```css
--color-primary: #3F4F32;
--color-primary-dark: #2F3D26;
--color-primary-light: #EEF2E8;
--color-secondary: #8A9574;
--color-accent: #D99A3D;
--color-background: #FAF8F2;
--color-surface: #FFFFFF;
--color-surface-soft: #F4F1E8;
--color-text-primary: #1E1E1E;
--color-text-secondary: #666666;
--color-text-muted: #999999;
--color-border: #E2DED3;
--color-danger: #D9534F;
--color-success: #4E7D45;
```

## Mock Data / API 연결 원칙

- API 응답이 있으면 DB와 연결된 API 데이터를 우선 사용합니다.
- API 연결 전/오프라인 환경에서는 `frontend/src/constants/mockData.js`의 예시 데이터로 화면 흐름을 유지합니다.
- mockData의 키는 DB 컬럼 의미를 유지한 camelCase 또는 기존 API 응답 키를 사용합니다.
- DB 스키마 snake_case ↔ 프론트/백엔드 camelCase 매핑은 `docs/developMe.md`에 유지합니다.

## 보호 규칙

- 크롤링/수집 백엔드는 사용자가 명시적으로 요청하지 않는 한 변경하지 않습니다.
- 보호 패키지: `backend/src/main/java/com/teukgeupjeonsa/backend/collector/`
