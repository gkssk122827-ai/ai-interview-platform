# 프론트엔드 실행 안내

## 개발 서버 실행

1. 의존성 설치

```bash
npm install
```

2. 개발 서버 실행

```bash
npm run dev
```

기본 접속 주소는 `http://localhost:5173`입니다.

## API 연동 방식

- 프론트엔드 API 클라이언트 기본 경로는 `/api/v1`입니다.
- 개발 서버에서 `/api` 요청은 `http://localhost:8080`으로 프록시됩니다.
- `frontend/.env`의 `VITE_USE_API_STUB=false`이면 백엔드 API를 사용합니다.

## 스텁 모드

백엔드 없이 화면 흐름만 확인하려면 `frontend/.env`에서 아래처럼 설정합니다.

```env
VITE_USE_API_STUB=true
```

스텁 로그인 계정:

- 이메일: `demo@aimentor.dev`
- 비밀번호: `password123`

## 빌드

```bash
npm run build
```
