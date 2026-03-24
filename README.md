# DevPort API

[devport.kr](https://devport.kr)의 백엔드 API 서버.

> 프로젝트 소개는 [devport-kr](https://github.com/devport-kr) 조직 페이지를 참고하세요.

## 기술 스택

| 분류       | 기술                                                 |
| ---------- | ---------------------------------------------------- |
| Framework  | Spring Boot 4.0.0, Java 25                           |
| Database   | PostgreSQL 16 (pgvector), Redis 7                    |
| ORM        | Spring Data JPA, QueryDSL 5.1                        |
| Auth       | Spring Security, OAuth2 (GitHub, Google, Naver), JWT |
| AI         | OpenAI SDK, pgvector                                 |
| Docs       | SpringDoc OpenAPI 3.0                                |
| Monitoring | Spring Actuator, Micrometer, CloudWatch              |
| Native     | GraalVM Native Image                                 |

## 프로젝트 구조

도메인 주도 설계(DDD) 패키지 구조를 사용합니다.

```
src/main/java/kr/devport/api/domain/
├── article/     # 게시글, 댓글, 검색, 자동완성
├── auth/        # 로그인, 회원가입, OAuth2, JWT, 프로필
├── wiki/        # 프로젝트 위키, AI 채팅 (벡터 검색), 세션
├── gitrepo/     # GitHub 저장소 브라우징, 트렌딩
├── llm/         # LLM 벤치마크 순위, 미디어 모델 순위
├── mypage/      # 북마크, 읽기 기록
├── port/        # 프로젝트 상세, 릴리즈 타임라인, 댓글/투표
└── common/      # 설정, 보안, 예외처리, 로깅, 캐시, 웹훅
```

각 도메인은 `controller/`, `service/`, `entity/`, `repository/`, `dto/` 하위 패키지로 구성됩니다.

## API 개요

기본 경로: `/api`

### Article (`/api/articles`)

| Method | Path                  | 설명                                                 |
| ------ | --------------------- | ---------------------------------------------------- |
| GET    | `/`                 | 게시글 목록 (페이지네이션, 카테고리 필터)            |
| GET    | `/search`           | 고급 검색 (카테고리, 소스, 키워드, 점수, 날짜, 태그) |
| GET    | `/search/fulltext`  | 전문 검색                                            |
| GET    | `/autocomplete`     | 자동완성 (최소 2자)                                  |
| GET    | `/trending-ticker`  | 트렌딩 티커                                          |
| GET    | `/{externalId}`     | 게시글 상세                                          |
| POST   | `/{articleId}/view` | 조회수 기록                                          |

### Article Comments (`/api/articles/{articleId}/comments`)

| Method | Path             | 설명                    |
| ------ | ---------------- | ----------------------- |
| GET    | `/`            | 댓글 목록               |
| POST   | `/`            | 댓글/답글 작성          |
| PUT    | `/{commentId}` | 댓글 수정               |
| DELETE | `/{commentId}` | 댓글 삭제 (soft delete) |

### Auth (`/api/auth`)

| Method | Path                 | 설명                     |
| ------ | -------------------- | ------------------------ |
| POST   | `/signup`          | 로컬 계정 가입           |
| POST   | `/login`           | 로컬 계정 로그인         |
| GET    | `/me`              | 현재 사용자 정보         |
| POST   | `/refresh`         | Access Token 갱신 (쿠키) |
| POST   | `/logout`          | 로그아웃                 |
| POST   | `/oauth2/exchange` | OAuth2 코드 교환         |
| POST   | `/verify-email`    | 이메일 인증              |
| POST   | `/forgot-password` | 비밀번호 재설정 요청     |
| POST   | `/reset-password`  | 비밀번호 재설정          |

### Profile (`/api/profile`)

| Method | Path                 | 설명                     |
| ------ | -------------------- | ------------------------ |
| PUT    | `/`                | 프로필 수정              |
| POST   | `/change-password` | 비밀번호 변경            |
| DELETE | `/email`           | 이메일 제거 (OAuth 전용) |
| PUT    | `/flair`           | 플레어 수정              |

### Wiki (`/api/wiki`)

| Method | Path                                   | 설명                        |
| ------ | -------------------------------------- | --------------------------- |
| GET    | `/projects`                          | 위키 프로젝트 목록          |
| GET    | `/projects/{externalId}`             | 프로젝트 위키 페이지        |
| POST   | `/projects/{externalId}/chat`        | 프로젝트 위키 채팅          |
| POST   | `/projects/{externalId}/chat/stream` | 스트리밍 채팅 (SSE)         |
| POST   | `/chat`                              | 글로벌 크로스 프로젝트 채팅 |
| POST   | `/chat/stream`                       | 글로벌 스트리밍 채팅        |

### Wiki Sessions (`/api/wiki/sessions`) — 인증 필요

| Method | Path                      | 설명             |
| ------ | ------------------------- | ---------------- |
| GET    | `/`                     | 전체 세션 목록   |
| GET    | `/project`              | 프로젝트별 세션  |
| GET    | `/global`               | 글로벌 채팅 세션 |
| GET    | `/{sessionId}/messages` | 세션 메시지 조회 |
| DELETE | `/{sessionId}`          | 세션 삭제        |

### Git Repos (`/api/git-repos`)

| Method | Path                     | 설명          |
| ------ | ------------------------ | ------------- |
| GET    | `/`                    | 저장소 목록   |
| GET    | `/trending`            | 트렌딩 저장소 |
| GET    | `/language/{language}` | 언어별 저장소 |

### LLM (`/api/llm`)

| Method | Path                             | 설명                                        |
| ------ | -------------------------------- | ------------------------------------------- |
| GET    | `/models`                      | LLM 모델 목록 (제공자, 라이선스, 가격 필터) |
| GET    | `/models/{modelId}`            | 모델 상세 + 벤치마크                        |
| GET    | `/models/search`               | 모델 고급 검색                              |
| GET    | `/leaderboard/{benchmarkType}` | 벤치마크별 리더보드                         |
| GET    | `/benchmarks`                  | 벤치마크 목록                               |
| GET    | `/media/text-to-image`         | Text-to-Image 모델                          |
| GET    | `/media/image-editing`         | Image Editing 모델                          |
| GET    | `/media/text-to-speech`        | Text-to-Speech 모델                         |
| GET    | `/media/text-to-video`         | Text-to-Video 모델                          |
| GET    | `/media/image-to-video`        | Image-to-Video 모델                         |

### MyPage (`/api/me`)

| Method | Path                            | 설명        |
| ------ | ------------------------------- | ----------- |
| GET    | `/saved-articles`             | 북마크 목록 |
| POST   | `/saved-articles/{articleId}` | 북마크 추가 |
| DELETE | `/saved-articles/{articleId}` | 북마크 제거 |
| GET    | `/read-history`               | 읽기 기록   |

### Projects (`/api/projects`)

| Method | Path                                       | 설명               |
| ------ | ------------------------------------------ | ------------------ |
| GET    | `/{id}`                                  | 프로젝트 상세      |
| GET    | `/{id}/events`                           | 릴리즈 타임라인    |
| GET    | `/comments`                              | 프로젝트 댓글 목록 |
| POST   | `/{projectId}/comments`                  | 댓글 작성          |
| POST   | `/{projectId}/comments/{commentId}/vote` | 투표 (+1, -1, 0)   |

### Admin (`/api/admin`) — 관리자 전용

게시글, 저장소, LLM 모델/벤치마크, 프로젝트, 사용자, 캐시, 위키 관리 엔드포인트.

### Webhook (`/api/webhooks/crawler`)

크롤러 작업 완료, LLM 처리 콜백 엔드포인트.

## 인증

- **JWT 이중 토큰**: Access Token (1시간, Authorization 헤더) + Refresh Token (30일, HttpOnly 쿠키)
- **OAuth2**: GitHub, Google, Naver
- **로컬 계정**: 이메일 가입 + 이메일 인증
- **Turnstile**: Cloudflare 봇 방지
- **위키 채팅**: 비인증 사용자 IP당 1일 1회 제한 (Redis)

## 데이터베이스

PostgreSQL 16 + pgvector 확장.

주요 테이블:

| 도메인  | 테이블                                                                                                       |
| ------- | ------------------------------------------------------------------------------------------------------------ |
| Auth    | `users`, `refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`                      |
| Article | `articles`, `article_metadata`, `article_comments`                                                     |
| Wiki    | `wiki_chat_sessions`, `wiki_chat_messages`, `wiki_section_chunks` (vector), `project_wiki_snapshots` |
| GitRepo | `git_repos`                                                                                                |
| LLM     | `llm_models`, `llm_benchmarks`, `llm_benchmark_scores`, `model_creators`, 미디어 모델 테이블 5개     |
| MyPage  | `user_saved_articles`, `user_read_history`                                                               |
| Port    | `projects`, `project_comments`, `project_comment_votes`, `project_events`, `project_metrics_daily` |

마이그레이션 파일은 `docs/migrations/`에 있습니다.
