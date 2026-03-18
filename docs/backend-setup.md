# 백엔드 설정

## 패키지 구조

백엔드 패키지는 `com.aimentor`를 루트로 하는 기능 중심 구조를 따릅니다.

```text
com.aimentor
|- BackendApplication
|- common
|  |- api
|  |  \- ApiResponse
|  |- config
|  |  \- SecurityConfig
|  |- entity
|  |  \- BaseTimeEntity
|  \- exception
|     \- GlobalExceptionHandler
|- domain.user
|- domain.profile
|- domain.interview
|- domain.learning
|- domain.report
|- domain.recommendation
|- external.ai
\- external.speech
```

### 참고

- `common`은 모든 기능에서 공통으로 사용하는 횡단 관심 인프라를 담습니다.
- `domain.*` 패키지는 기능별로 controller, service, repository, dto, entity를 소유합니다.
- `external.*` 패키지는 AI, 음성 서비스 같은 외부 연동을 위한 영역입니다.
- 인증 로직은 단계적으로 확장할 예정이며, 현재 보안 설정은 기본 형태입니다.

## 로컬 실행

### 1. MariaDB 데이터베이스 생성

```sql
CREATE DATABASE aimentor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 로컬 환경 파일 생성

`backend/.env.example`을 `backend/.env`로 복사한 뒤 값을 수정합니다.

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aimentor
DB_USERNAME=your_mariadb_username
DB_PASSWORD=your_mariadb_password
SPRING_PROFILES_ACTIVE=local
JWT_SECRET_KEY=your_generated_secret
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### 3. JWT 시크릿 키 생성

PowerShell 예시:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 } | ForEach-Object { [byte]$_ }))
```

### 4. 백엔드 실행

`backend` 디렉터리에서 아래 명령을 실행합니다.

```powershell
.\scripts\start-local.ps1
```

스크립트는 `.env` 값을 읽고 아래 명령을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

### 5. 기본 URL

- 백엔드 API: `http://localhost:8080`

### 참고 사항

- AI와 음성 연동은 기본적으로 스텁 구현을 사용하므로 로컬 실행 시 실제 API 키가 필요하지 않습니다.
- MariaDB 데이터소스 설정은 `application-local.yml`에 분리되어 있습니다.
- 로컬 실행 시 `SPRING_PROFILES_ACTIVE=local`을 사용해야 합니다.
- MariaDB 접속 정보가 잘못되면 데이터소스 생성 단계에서 애플리케이션이 실패합니다.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update`는 로컬 개발 환경에서만 사용하는 것이 적절합니다.
