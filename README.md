# DevPort Backend API

Backend API for DevPort (devport.kr) - Korean-language developer content aggregator.

## Tech Stack

- **Spring Boot 4.0.0** with Java 25
- **Spring Data JPA** for database operations
- **Spring Security** with OAuth2 (GitHub, Google)
- **JWT** for authentication
- **Gradle** for build management
- **Lombok** for boilerplate reduction
- **H2** (development), **PostgreSQL/MySQL** (production)

## Project Structure

```
src/main/java/kr/devport/api/
├── DevportApiApplication.java        # Main Spring Boot application
├── config/
│   ├── CorsConfig.java              # CORS configuration for React frontend
│   └── SecurityConfig.java          # Spring Security + OAuth2 configuration
├── domain/
│   ├── entity/                      # JPA entities
│   │   ├── Article.java             # Main article entity
│   │   ├── ArticleMetadata.java     # Embedded metadata
│   │   ├── LLMModel.java            # LLM model entity
│   │   ├── LLMBenchmarkScore.java   # Benchmark scores
│   │   ├── Benchmark.java           # Benchmark reference data
│   │   └── User.java                # User entity for authentication
│   └── enums/                       # Domain enums
│       ├── ItemType.java            # REPO, BLOG, DISCUSSION
│       ├── Source.java              # github, hackernews, reddit, etc.
│       ├── Category.java            # AI_LLM, DEVOPS_SRE, etc.
│       ├── BenchmarkType.java       # AGENTIC_CODING, REASONING, etc.
│       ├── AuthProvider.java        # github, google
│       └── UserRole.java            # USER, ADMIN
├── repository/                      # Spring Data JPA repositories
│   ├── ArticleRepository.java
│   ├── LLMModelRepository.java
│   ├── LLMBenchmarkScoreRepository.java
│   ├── BenchmarkRepository.java
│   └── UserRepository.java
├── security/                        # Security components
│   ├── JwtTokenProvider.java        # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java # JWT authentication filter
│   ├── CustomUserDetails.java       # Custom user details
│   └── oauth2/                      # OAuth2 specific classes
│       ├── OAuth2UserInfo.java
│       ├── GitHubOAuth2UserInfo.java
│       ├── GoogleOAuth2UserInfo.java
│       ├── OAuth2UserInfoFactory.java
│       ├── CustomOAuth2UserService.java
│       ├── OAuth2AuthenticationSuccessHandler.java
│       └── OAuth2AuthenticationFailureHandler.java
├── service/                         # Business logic layer
│   ├── ArticleService.java
│   ├── LLMRankingService.java
│   ├── AuthService.java             # Authentication service
│   ├── RefreshTokenService.java     # Refresh token management
│   └── TurnstileService.java        # Cloudflare Turnstile validation
├── controller/                      # REST API controllers
│   ├── ArticleController.java
│   ├── LLMRankingController.java
│   └── AuthController.java          # Authentication endpoints
└── dto/
    ├── request/                     # Request DTOs
    │   ├── RefreshTokenRequest.java
    │   └── TurnstileValidationRequest.java
    └── response/                    # Response DTOs
        ├── ArticleResponse.java
        ├── ArticlePageResponse.java
        ├── ArticleMetadataResponse.java
        ├── TrendingTickerResponse.java
        ├── LLMModelResponse.java
        ├── LLMRankingResponse.java
        ├── BenchmarkResponse.java
        ├── UserResponse.java
        ├── AuthResponse.java
        ├── TokenResponse.java
        └── TurnstileValidationResponse.java
```

## API Endpoints

### Articles

#### 1. Get Articles (Main Feed)
```
GET /api/articles?category={category}&page={page}&size={size}
```
- **Query Parameters:**
  - `category` (optional): `AI_LLM`, `DEVOPS_SRE`, `BACKEND`, `INFRA_CLOUD`, `OTHER`
  - `page` (optional, default: 0): Page number
  - `size` (optional, default: 9): Items per page
- **Response:** Paginated article list with `content`, `totalElements`, `totalPages`, `currentPage`, `hasMore`

#### 2. Get GitHub Trending Repos
```
GET /api/articles/github-trending?limit={limit}
```
- **Query Parameters:**
  - `limit` (optional, default: 10): Number of repos to return
- **Response:** List of GitHub repositories sorted by score/stars

#### 3. Get Trending Ticker Articles
```
GET /api/articles/trending-ticker?limit={limit}
```
- **Query Parameters:**
  - `limit` (optional, default: 20): Number of articles for ticker
- **Response:** List of trending articles for auto-scrolling ticker

### Authentication

#### 4. Get Current User
```
GET /api/auth/me
```
- **Headers:** `Authorization: Bearer {jwt_token}`
- **Response:** Current authenticated user information

### LLM Rankings

#### 5. Get LLM Benchmark Rankings
```
GET /api/llm-rankings?benchmark={benchmark}&limit={limit}
```
- **Query Parameters:**
  - `benchmark` (optional, default: `AGENTIC_CODING`): Benchmark type
    - Options: `AGENTIC_CODING`, `REASONING`, `MATH`, `VISUAL`, `MULTILINGUAL`
  - `limit` (optional, default: 8): Number of models to return
- **Response:** Benchmark info + top N models with scores and ranks

#### 6. Get All Benchmarks (Reference)
```
GET /api/benchmarks
```
- **Response:** List of all benchmark configurations (metadata for frontend)

## Authentication Flow

### OAuth2 Login Flow (with Bot Protection)

1. **Frontend bot verification:**
   - User completes Cloudflare Turnstile captcha on login page
   - Frontend stores Turnstile token in `turnstile_token` cookie
   - User clicks "Login with GitHub" or "Login with Google"

2. **Frontend initiates OAuth2:**
   - Redirect to: `http://localhost:8080/oauth2/authorization/{provider}`
   - Provider options: `github`, `google`

3. **OAuth2 provider authentication:**
   - User authenticates with GitHub/Google
   - Provider redirects back to backend with authorization code

4. **Backend processes authentication:**
   - `CustomOAuth2UserService` loads user info from provider
   - Creates or updates `User` entity in database
   - `OAuth2AuthenticationSuccessHandler` validates Turnstile token from cookie
     - Calls `TurnstileService` to verify with Cloudflare API
     - Blocks login if token is invalid or missing (redirects to error page)
   - If valid, generates JWT tokens (Access + Refresh)

5. **Frontend receives tokens:**
   - User redirected to: `http://localhost:5173/oauth2/redirect?accessToken={xxx}&refreshToken={yyy}`
   - Frontend stores both tokens (localStorage/sessionStorage)

6. **Authenticated API requests:**
   - Frontend includes access token: `Authorization: Bearer {access_token}`
   - `JwtAuthenticationFilter` validates token
   - Request proceeds with authenticated user context

7. **Token refresh (automatic):**
   - When access token expires (1 hour), frontend calls `/api/auth/refresh`
   - Backend returns new access token
   - User stays logged in for 30 days

### Setting Up OAuth2 Providers

#### GitHub OAuth App
1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Create new OAuth App with:
   - **Homepage URL:** `http://localhost:8080` (dev) or `https://devport.kr` (prod)
   - **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`
3. Copy Client ID and Client Secret to `.env` file

#### Google OAuth Client
1. Go to Google Cloud Console → APIs & Services → Credentials
2. Create OAuth 2.0 Client ID with:
   - **Application type:** Web application
   - **Authorized redirect URIs:** `http://localhost:8080/login/oauth2/code/google`
3. Copy Client ID and Client Secret to `.env` file

### Environment Variables

Copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

Required variables:
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret
- `GITHUB_CLIENT_ID` - GitHub OAuth client ID
- `GITHUB_CLIENT_SECRET` - GitHub OAuth client secret
- `JWT_SECRET` - Secret key for JWT signing (minimum 256 bits)
- `CLOUDFLARE_TURNSTILE_SECRET_KEY` - Cloudflare Turnstile secret key for bot protection

Generate JWT secret:
```bash
openssl rand -base64 64
```

Get Cloudflare Turnstile keys:
1. Sign up at https://dash.cloudflare.com/
2. Go to Turnstile section
3. Create a new site
4. Copy the **Site Key** (for frontend) and **Secret Key** (for backend)

## Database Schema

### Users
- **users** - User accounts from OAuth2
  - `id`, `email`, `name`, `profile_image_url`
  - `auth_provider` (github/google), `provider_id`
  - `role` (USER/ADMIN)
  - `created_at`, `updated_at`, `last_login_at`

### Articles
- **articles** - Main article table
  - `id`, `item_type`, `source`, `category`
  - `summary_ko_title`, `summary_ko_body`, `title_en`
  - `url`, `score`, `created_at_source`, `created_at`, `updated_at`
  - Embedded metadata: `stars`, `comments`, `upvotes`, `read_time`, `language`
- **article_tags** - Many-to-many tags

### LLM Leaderboard
- **llm_models** - LLM model definitions
  - `id`, `name`, `provider`, `context_window`, `pricing`
- **llm_benchmark_scores** - Benchmark scores for each model
  - `id`, `model_id`, `benchmark_type`, `score`, `rank`, `updated_at`
  - Unique constraint: `(model_id, benchmark_type)`
- **benchmarks** - Benchmark reference data (5 types)
  - `type` (PK), `label_en`, `label_ko`, `description_en`, `description_ko`, `icon`

## Configuration

### Development (application.yml)
- H2 in-memory database
- H2 console enabled at `/h2-console`
- SQL logging enabled
- CORS enabled for `http://localhost:5173` (Vite)
- OAuth2 redirect URI: `http://localhost:5173/oauth2/redirect`
- JWT expiration: 24 hours

### Production (application-prod.yml)
- PostgreSQL/MySQL database
- SQL logging disabled
- CORS enabled for `https://devport.kr`
- OAuth2 redirect URI: `https://devport.kr/oauth2/redirect`
- JWT expiration: 24 hours

## Running the Application

### Prerequisites
1. Set up environment variables (copy `.env.example` to `.env`)
2. Configure OAuth2 apps on GitHub and Google
3. Generate JWT secret

### Run

```bash
# Development mode
./gradlew bootRun

# Build
./gradlew build

# Run tests
./gradlew test

# Run with production profile
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Next Steps

1. **Populate Database** - Add sample data for testing (articles, LLM models, benchmarks)
2. **Data Aggregation** - Implement scrapers for GitHub, Hacker News, Reddit, etc.
3. **Korean Summarization** - Integrate LLM API for auto-summarization
4. **Scheduled Jobs** - Set up periodic scraping tasks
5. **Caching** - Add Redis for trending ticker and GitHub leaderboard
6. **Testing** - Write unit and integration tests

## Frontend Integration

The React frontend expects:
- API base URL: `http://localhost:8080/api` (development)
- CORS enabled for `http://localhost:5173`
- ISO 8601 date format: `2024-01-15T10:30:00Z`
- Pagination with `hasMore` flag for infinite scroll
- OAuth2 login redirect to: `http://localhost:5173/oauth2/redirect?token={jwt}`
- Authenticated requests include header: `Authorization: Bearer {jwt}`

## Notes

- All entities use Lombok (`@Getter`, `@Setter`, `@Builder`, etc.)
- Services are transactional with `@Transactional(readOnly = true)`
- Repository methods use Spring Data JPA query derivation
- DTOs separate internal entities from API contracts
- CORS configured for React dev server and production domain
- OAuth2 authentication with GitHub and Google
- Stateless JWT-based session management
- Public endpoints: `/api/articles/**`, `/api/llm-rankings`, `/api/benchmarks`
- Protected endpoints: `/api/auth/me` (requires authentication)

---

**Created:** 2025-01-23
**Spring Boot Version:** 4.0.0
**Java Version:** 25
