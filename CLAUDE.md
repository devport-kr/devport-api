# CLAUDE.md — devport-api conventions

Kotlin/JVM, multi-module Gradle, **strict hexagonal** architecture. One Spring Boot app is
assembled from per-domain slices at the `:application-api` composition root. These rules are enforced
by `application-api/src/test/kotlin/kr/devport/api/architecture/ArchitectureBoundaryTest.kt` (Konsist)
— if you change layering, run it.

## Domains & layering

Domains: `auth`, `article`, `gitrepo`, `llm`, `mypage`, `port`, `wiki`. Shared: `support:{cache,
security, exception, logging}`. Each domain is a set of Gradle modules, one per layer:

```
<domain>:model            entities (JPA) + enums + cross-domain View models. No framework beyond JPA.
<domain>:infrastructure   PORTS ONLY: interfaces + pure domain types the ports speak. No impls.
<domain>:repository-jpa   Spring Data interfaces + @Repository adapters implementing the persistence ports.
<domain>:adapter-<tech>   driven adapters (openai/redis/http) implementing the external ports.
<domain>:service          use-cases + DTOs. Depends on ports only.
<domain>:api              @RestController + web/SSE. Depends on service only.
```

Not every domain has every module (e.g. only some have `adapter-*`). `gitrepo` is the original
reference slice and is pure (`type=kotlin`, package `kr.devport.gitrepo`).

## Dependency direction (hard rules)

- **`service` depends on `infrastructure` (ports) only.** It must NOT import `..repository..`,
  `org.springframework.data.jpa.repository.JpaRepository`, `com.openai.*`,
  `org.springframework.data.redis.*`, `org.springframework.web.*`, or `jakarta.servlet.*`.
- **`api` depends on `service`.** It must NOT import `..repository..`.
- **`model` must NOT import another domain's `kr.devport.api.domain.<other>`** types.
- **`repository-jpa` / `adapter-*` depend on their own `infrastructure`** and implement its ports.
- **`application-api`** is the only module that wires everything; it owns `@SpringBootApplication`,
  `@EntityScan`/`@EnableJpaRepositories` (`kr.devport.api`, `kr.devport.gitrepo`), and shared beans
  (e.g. `OpenAIClient`, `SecurityConfig`, `RedisTemplate`).

## Ports & adapters

- **Out-ports (driven)** live in `<domain>:infrastructure` as interfaces — persistence repositories
  and external services (LLM `ChatPort`/`EmbeddingPort`, `WikiChatSessionStore`, `RateLimitCounter`,
  `GitHubRepoFetcher`, …). Implemented by `repository-jpa` / `adapter-*`.
- **Adapters own all framework/vendor types.** Keep `com.openai.*`, Redis, raw HTTP, etc. inside the
  adapter; the port speaks pure domain types defined in `infrastructure` (e.g. `ChatMessage`,
  `ChatRole`, `JsonSchemaSpec`, `ScoredChunkRow`). The core never sees vendor classes.
- **Persistence port pattern**: `XxxRepository` (port, in `infrastructure`) ←
  `XxxRepositoryAdapter` (`@Repository`, in `repository-jpa`) → delegates to a Spring Data
  `XxxJpaRepository`. Custom SQL (e.g. pgvector via `JdbcTemplate`) goes in a separate `@Component`
  the adapter delegates to — do not rely on Spring Data fragment-name weaving.

## Cross-domain access

No cross-domain entity references and no cross-domain `@ManyToOne`. Instead:

- Reference another domain by **id** (`var userId: Long`), nullable where optional.
- Resolve to a **pure View model** via the owning domain's **inbound `Directory` port**:
  `UserDirectory`→`UserSummary` (auth), `ArticleDirectory`→`ArticleView` (article),
  `ProjectDirectory`→`ProjectView` (port). The `XxxDirectory` interface lives in
  `<domain>:infrastructure` and is implemented by `<domain>:service`; the View lives in
  `<domain>:model`. Consumers depend only on the interface + View.

Entities deliberately stay in `model` (pragmatic variant) — within a domain, ports may return
entities; across domains, only Views cross the boundary.

## Build system

Module config is driven by the `type=` token in each module's `gradle.properties` (LINE
build-recipe), not by per-module plugin blocks. Set `type=` + `group=kr.devport.<domain>`:

- `kotlin` — plain Kotlin + ktlint + test suites
- `kotlin-boot` — + Spring Boot BOM/starter (infrastructure, service, adapters)
- `kotlin-boot-jpa` — + Spring Data JPA + hibernate-vector (model)
- `kotlin-boot-jpa-repository` — repository-jpa
- `kotlin-boot-mvc` — + Spring Web/validation/springdoc (api)
- `kotlin-boot-mvc-application` — the bootable app (application-api)

When you add a module: create `gradle.properties` (`type=`/`group=`) + `build.gradle.kts`, then
`include(":<domain>:<module>")` in `settings.gradle.kts`, and add it to `application-api` deps if it
must be on the runtime/bean path. Pin versions in `gradle/libs.versions.toml`.

## Kotlin / lint

- Kotlin 2.2.x, JVM target 24 (JDK 25 toolchain). **ktlint is enforced — the build fails on
  violations.** Run `./gradlew ktlintFormat` before committing.
- Spring Data derived-query methods with `_` (e.g. `findByComment_ExternalId...`) require
  `@file:Suppress("ktlint:standard:function-naming")` at the top of the file.

## Testing

- **Unit tests**: Mockito (`mockito-kotlin`); mock the **ports**, not infrastructure.
- **Integration / boot**: `@SpringBootTest @ActiveProfiles("test")` with **Testcontainers**
  (`pgvector/pgvector` + redis) via `@ServiceConnection`; annotate
  `@Testcontainers(disabledWithoutDocker = true)` so the default build stays green without Docker.
  `application-test.yml` supplies dummy secrets so the full context resolves; the datasource/redis
  come from the containers. There is no H2 — wiki uses pgvector/pg_trgm which H2 cannot model.
- `RagEvalRunner` is an **opt-in** harness (`@Tag("rag-eval")`, needs a real populated Postgres via
  `RAG_EVAL_DATABASE_URL`); run with `./gradlew :application-api:ragEval`. Do not migrate it to an
  empty Testcontainers DB — its eval needs an ingested corpus.

## Verify (green gate)

```
./gradlew build                                   # compile + ktlint + tests across all modules
./gradlew :application-api:test --tests "*ArchitectureBoundaryTest"   # layering guard
./gradlew :<domain>:service:dependencies          # confirm no repository-jpa/adapter/openai/redis/web leaks
```

## Gotchas

- **Postgres operator precedence**: pg_trgm `%` binds tighter than `->>`. Always parenthesize:
  `(c.metadata->>'titleKo') % ?`, never `c.metadata->>'titleKo' % ?` (parses as `jsonb ->> boolean`).
- A derived `deleteBy…` needs an active transaction — call it from an `@Transactional` method.

## Commits

Korean conventional commits: `type(scope): 설명` (e.g. `refactor(wiki): out-port + 디커플링`).
Commit/push only when asked.
