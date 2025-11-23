# DevPort Backend API

Backend API for DevPort (devport.kr) - Korean-language developer content aggregator.

## Tech Stack

- **Spring Boot 4.0.0** with Java 25
- **Spring Data JPA** for database operations
- **Gradle** for build management
- **Lombok** for boilerplate reduction
- **H2** (development), **PostgreSQL/MySQL** (production)

## Project Structure

```
src/main/java/kr/devport/api/
├── DevportApiApplication.java        # Main Spring Boot application
├── config/
│   └── CorsConfig.java              # CORS configuration for React frontend
├── domain/
│   ├── entity/                      # JPA entities
│   │   ├── Article.java             # Main article entity
│   │   ├── ArticleMetadata.java     # Embedded metadata
│   │   ├── LLMModel.java            # LLM model entity
│   │   ├── LLMBenchmarkScore.java   # Benchmark scores
│   │   └── Benchmark.java           # Benchmark reference data
│   └── enums/                       # Domain enums
│       ├── ItemType.java            # REPO, BLOG, DISCUSSION
│       ├── Source.java              # github, hackernews, reddit, etc.
│       ├── Category.java            # AI_LLM, DEVOPS_SRE, etc.
│       └── BenchmarkType.java       # AGENTIC_CODING, REASONING, etc.
├── repository/                      # Spring Data JPA repositories
│   ├── ArticleRepository.java
│   ├── LLMModelRepository.java
│   ├── LLMBenchmarkScoreRepository.java
│   └── BenchmarkRepository.java
├── service/                         # Business logic layer
│   ├── ArticleService.java
│   └── LLMRankingService.java
├── controller/                      # REST API controllers
│   ├── ArticleController.java
│   └── LLMRankingController.java
└── dto/
    └── response/                    # API response DTOs
        ├── ArticleResponse.java
        ├── ArticlePageResponse.java
        ├── ArticleMetadataResponse.java
        ├── TrendingTickerResponse.java
        ├── LLMModelResponse.java
        ├── LLMRankingResponse.java
        └── BenchmarkResponse.java
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

### LLM Rankings

#### 4. Get LLM Benchmark Rankings
```
GET /api/llm-rankings?benchmark={benchmark}&limit={limit}
```
- **Query Parameters:**
  - `benchmark` (optional, default: `AGENTIC_CODING`): Benchmark type
    - Options: `AGENTIC_CODING`, `REASONING`, `MATH`, `VISUAL`, `MULTILINGUAL`
  - `limit` (optional, default: 8): Number of models to return
- **Response:** Benchmark info + top N models with scores and ranks

#### 5. Get All Benchmarks (Reference)
```
GET /api/benchmarks
```
- **Response:** List of all benchmark configurations (metadata for frontend)

## Database Schema

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

### Production (application-prod.yml)
- PostgreSQL/MySQL database
- SQL logging disabled
- CORS enabled for `https://devport.kr`

## Running the Application

```bash
# Development mode
./gradlew bootRun

# Build
./gradlew build

# Run tests
./gradlew test
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

## Notes

- All entities use Lombok (`@Getter`, `@Setter`, `@Builder`, etc.)
- Services are transactional with `@Transactional(readOnly = true)`
- Repository methods use Spring Data JPA query derivation
- DTOs separate internal entities from API contracts
- CORS configured for React dev server and production domain

---

**Created:** 2025-01-23
**Spring Boot Version:** 4.0.0
**Java Version:** 25
