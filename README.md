# Cost Analysis WAS

견적서 분석 시스템 백엔드 API 서버

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.4 |
| DB | PostgreSQL 16 |
| Cache/Session | Redis |
| Storage | MinIO (S3 호환) |
| Auth | JWT (jjwt 0.12.5) + Redis Blacklist |
| AI | Claude API (`claude-opus-4-6`) via WebClient SSE |
| File Parsing | Apache POI (Excel), PDFBox 3 (PDF) |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Infra | Docker Compose |

## 도메인 구조

```
src/main/java/com/costanalysis/
├── domain/
│   ├── auth/          POST /api/v1/auth/login, /logout, /refresh
│   ├── user/          GET/PUT /api/v1/users/me, ADMIN CRUD
│   ├── quotation/     파일 업로드 + SSE 파싱 진행률 + CRUD
│   ├── verification/  AI 자동 검증 + 승인/반려
│   ├── analysis/      카테고리별 집계 + 이상값 탐지
│   ├── comparison/    다중 견적서 비교
│   ├── insight/       Claude API SSE 채팅 (Extended Thinking)
│   ├── model/         원가 수식 관리 (CRUD)
│   ├── notification/  인앱 알림 + 활동 로그
│   └── dashboard/     통계 대시보드
├── global/
│   ├── config/        Security, CORS, WebClient, MinIO, OpenAPI
│   ├── exception/     BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── response/      ApiResponse<T>
│   ├── security/      JwtTokenProvider, JwtAuthenticationFilter
│   └── storage/       FileStorageService (MinIO)
```

## 주요 API

### 인증
- `POST /api/v1/auth/login` — 사번/비밀번호 → JWT 발급
- `POST /api/v1/auth/logout` — Access Token 블랙리스트 등록
- `POST /api/v1/auth/refresh` — Refresh Token → 새 토큰 발급

### 견적서
- `POST /api/v1/quotations/upload` — 파일 업로드 (PDF/XLSX) → quotationId 반환
- `GET  /api/v1/quotations/{id}/parse` — SSE: 파싱 진행률 (`progress`, `done`, `error`)
- `GET  /api/v1/quotations/{id}` — 상세 + 파싱 항목 + 다운로드 URL
- `PATCH /api/v1/quotations/items/{itemId}` — 파싱 항목 수정

### AI 인사이트
- `POST /api/v1/insight/sessions/{id}/chat` — SSE 채팅 스트리밍
  - 이벤트: `delta` (텍스트), `thinking` (Extended Thinking), `done`, `error`

### 분석
- `GET /api/v1/quotations/{id}/analysis` — 카테고리 집계 + 이상값 탐지

## 실행

```bash
# 인프라 서비스 실행 (PostgreSQL, Redis, MinIO)
docker compose up -d postgres redis minio

# 앱 실행 (개발)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

## 기본 관리자 계정

| 사번 | 비밀번호 |
|------|---------|
| ADMIN001 | Admin1234! |

(V2__seed_data.sql에서 BCrypt 해시로 초기 생성)
