# DevPort API — Kotlin Hexagonal Migration Report

**Status: ✅ Complete.** The single‑module Java Spring Boot application has been refactored into a
Gradle **multi‑module Kotlin Hexagonal (Ports & Adapters) monolith** where module boundaries are
**compiler‑enforced**. The codebase is now **100 % Kotlin** (0 production `.java` files), GraalVM
native‑image support has been fully removed, and `./gradlew build` is green (compile + ktlint +
tests + `bootJar`), with the `contextLoads` `@SpringBootTest` booting the fully composed app.

---

## 1. Goal & approach

- **Goal:** every domain in its own set of Kotlin modules, with the rule **`api ↛ repository-jpa`**
  (an HTTP adapter cannot reach a persistence adapter) enforced by Gradle dependency scoping, not by
  convention.
- **Approach:** a *strangler* migration. A temporary `:legacy` module held all not‑yet‑migrated Java
  while domains were peeled off one at a time; the build stayed green at every step; `:legacy` was
  deleted once empty.
- **Key decisions (carried over):** persistence stays **JPA/QueryDSL/pgvector** (in `repository-jpa`
  adapters); **JVM only** (native image was dropped mid‑project); schema unchanged (Hibernate
  `ddl-auto` + `init-scripts`); cross‑cutting concerns extracted into `support:*`; the bootable app is
  the `application-api` composition root.

---

## 2. Final module layout (34 Gradle modules)

```
support:cache         support:security      support:exception     support:logging      (cross-cutting)

<domain>/
  model            (type=kotlin-boot-jpa)            @Entity + enums (+ QueryDSL kapt where needed)
  repository-jpa   (type=kotlin-boot-jpa-repository) Spring Data repos + QueryDSL/JdbcTemplate adapters
  service          (type=kotlin-boot)                use-cases + all DTOs
  api              (type=kotlin-boot-mvc)            controllers only

domains: gitrepo (+infrastructure), llm, auth, article, mypage, port, wiki
application-api  (type=kotlin-boot-mvc-application)  composition root: SecurityConfig, config beans,
                                                     webhook, GlobalExceptionHandler, the test suite
```

**Dependency direction per domain:** `service` depends on `repository-jpa` via `implementation`
(non‑transitive); `api` depends on `service` via `implementation`. Therefore **`api` cannot see
`repository-jpa`** — the core hexagonal invariant, enforced by the compiler.

Cross‑domain references use another domain's `model`/`service`/`repository-jpa` as a **shared
kernel**, and are confined to the **service** layer (e.g. `mypage:service` → `article:repository-jpa`,
`wiki:service` → `port:repository-jpa` + `auth:repository-jpa`).

---

## 3. What was migrated

| Area | From | To |
|---|---|---|
| gitrepo, llm, auth | Java (legacy) | Kotlin slices (earlier phases) |
| **article + mypage** | Java (legacy) | Kotlin slices (QueryDSL kept via `kapt` in `article:model`) |
| **port** | Java (legacy) | Kotlin slice (QueryDSL via `kapt`) |
| **wiki** | Java (legacy) | Kotlin slice — entities (pgvector `FloatArray` + jsonb), JdbcTemplate pgvector repo, RAG/OpenAI services, Redis session store, 5 SSE controllers — **all Kotlin** |
| common/config, webhook, GlobalExceptionHandler | Java → `application-api` | Kotlin |
| common/cache, security, exception, logging | Java → `support:*` | Kotlin |
| `ArticleLLMService` (OpenAI) | Java | Kotlin |
| `:legacy` strangler module | — | **deleted** |
| GraalVM native image (config, metadata, CI, reflection hints) | present | **removed** |

The boundary check passes for **every** domain: each `<d>:api`'s `compileClasspath` contains **zero**
`repository-jpa` artifacts (verified with `./gradlew :<d>:api:dependencies`).

---

## 4. Conventions used (apply these to any future domain)

- **Packages stay `kr.devport.api.domain.<domain>.*`** so the composition root's component/entity/repo
  scanning picks everything up — no per‑module `AutoConfiguration`/`.imports` needed (only `gitrepo`,
  the reference slice, uses the LINE‑recipe `@AutoConfiguration` + `kr.devport.gitrepo` package).
- **The Kotlin `@Entity` *is* the model** (no separate model + mapper). JPA annotations on `var`
  properties (no `@field:`); `@PrePersist`/`@PreUpdate` as `protected fun`; `nullable var createdAt`
  set in lifecycle callbacks.
- **DTOs are Kotlin `data class`es in the `service` module**, with `@Schema` (springdoc) dropped
  (service modules have no web dep); controllers in `api` keep `@Operation`/`@Tag`.
- **Caching:** services keep `@Cacheable`/`@CacheEvict`; only `CacheNames` is a compile dep
  (`support:cache`); `@cacheKeyFactory` / `@cacheFallbackBypass` resolve at runtime via SpEL. Domain
  enums are passed to the shared `CacheKeyFactory` as their `String` name (`#category?.name()`).
- **QueryDSL** is retained via `kapt` in the `model` module (generates `QArticle`, `QProject`, …);
  cross‑domain `Q`‑classes (e.g. `QArticleComment` → `QUser`) require the other domain's `model` on
  the kapt classpath. `JPAQueryFactory` bean lives in `application-api` (`QuerydslConfig`).
- **Spring Data derived queries with `_` traversal** (`findByUserIdAndArticle_ExternalId`) need
  `@file:Suppress("ktlint:standard:function-naming")`.

---

## 5. Notable decisions, deviations & gotchas

- **`ArticleSearchCondition` lives in `article:model`** (not `service`): the QueryDSL adapter in
  `repository-jpa` references it, and `repository-jpa` must not depend on `service` (would be a cycle).
- **`port` is self‑contained:** it does *not* depend on `article:service` (it has its own comment
  DTOs), which is cleaner than the original plan.
- **wiki was fully converted to Kotlin** (≈3 000 LOC including the OpenAI/RAG/streaming path and the
  pgvector JdbcTemplate repo) and its 14 Java test files were **ported to Kotlin** (added
  `org.mockito.kotlin:mockito-kotlin`), preserving the RAG/chat coverage rather than deleting it.
- **Wire‑format preservation:**
  - Redis cache serialization (`RedisConfig`) keeps the exact Jackson config (`activateDefaultTyping`
    `NON_FINAL` + `@class` property typing) and DTO **FQNs are unchanged**, so previously cached Redis
    entries still deserialize. `StartupCacheEvictor` also flushes Spring cache regions on boot.
  - `WikiChatResponse.isClarification` / `WikiMessageResponse.isClarification` use
    `@get:JsonProperty("isClarification")`; `ArticleLLMPreviewResponse.technical` matches the original
    Lombok `boolean isTechnical` → `"technical"` JSON name.
  - Webhook `CrawlerJobCompletedRequest` snake_case (`job_id`, …) preserved via `@param:JsonProperty`.
- **Spring Security 6 nullability:** `UserDetails.getUsername()/getName()/getAuthorities()` are
  non‑null in Kotlin, so `CustomUserDetails` returns `?: ""` / `emptyList()`; `getPassword()` /
  `getAttributes()` stay nullable. `CustomUserDetails.id` is non‑null `Long` (the factory uses
  `requireNotNull(user.id)`).
- **`JwtTokenProvider`** keeps `generate*/validateToken/getUserIdFromToken` parameters **nullable** to
  match the Java platform‑type leniency the existing auth code relied on.
- **Lombok removed from every module build.** **GraalVM/native removed everywhere** (config,
  `META-INF/native-image` metadata, `build-native-*.yml` workflows, `@RegisterReflectionForBinding`
  hints; `build-jvm.yml` now uses temurin JDK 25).
- **Mockito‑in‑Kotlin gotchas (for future tests):** `any()` ≠ `null` → use `anyOrNull()` for nullable
  args that are null at call time; `@InjectMocks` on a Kotlin class needs **all** constructor params
  mocked (Kotlin ctors are non‑null, unlike Java which tolerated nulls); build stub objects *before*
  `whenever(...).thenReturn(...)` (nested stubbing throws); AssertJ `.extracting { }` matches the
  throwing‑extractor overload → use `.map { }`.

---

## 6. Verification

- `./gradlew build` → **green**: compiles all 34 modules, `ktlint` clean, runs `test` +
  `integrationTest`, builds `bootJar`.
- `contextLoads` `@SpringBootTest` boots the fully composed application (all migrated modules +
  `SecurityConfig` + JPA entity/repo scanning across packages) — the key composition gate.
- Boundary acceptance: every `<d>:api` `compileClasspath` has **zero** `repository-jpa` — adding an
  `import …<Something>RepositoryImpl` inside an `api` module fails to compile.
- 100 % Kotlin: `find . -path '*/src/main/*' -name '*.java'` = **0**.

---

## 7. Things you should do next (recommended)

> Code is green; these are **runtime/operational** checks for the rewritten paths and a few optional
> hardening steps. Nothing is committed yet — review and commit when satisfied.

**Smoke‑test the rewritten runtime paths (highest priority):**
1. **OAuth2 login + JWT round‑trip** — `JwtTokenProvider` and `CustomUserDetails` were rewritten.
   Verify login, token issuance, `Bearer` auth on a protected endpoint, and refresh.
2. **Wiki SSE streaming** (`/api/wiki/projects/{id}/chat/stream`, `/api/wiki/chat/stream`) — the
   OpenAI streaming + virtual‑thread + SSE path is new Kotlin. Confirm tokens stream and the `done`
   event carries `sessionId`/`isClarification`.
3. **Wiki RAG quality** — run the opt‑in harness against a real Postgres:
   `RAG_EVAL_DATABASE_URL=… ./gradlew :application-api:ragEval` — to confirm hybrid retrieval/rerank
   behaves as before. (If you run it, ensure a Jackson Kotlin module is on the eval classpath; the
   runner uses `findAndRegisterModules()`.)
4. **Crawler webhook** — POST a real `job-completed` payload in staging to confirm snake_case
   (`job_id`, `signature`, …) deserializes via `@param:JsonProperty` and HMAC validation passes.
5. **Redis cache** — confirm cached reads still (de)serialize; the format and DTO FQNs are unchanged,
   and `StartupCacheEvictor` flushes Spring regions on boot, so this should be transparent.

**Optional hardening / follow‑ups:**
6. **Lock the boundary permanently** with an ArchUnit or Konsist test (today `api ↛ repository-jpa`
   is enforced only by Gradle deps; a test makes regressions obvious in CI).
7. **Split the test suite per module** — it still lives entirely in `application-api`. Moving
   unit tests next to their modules would speed up incremental builds.
8. Re‑evaluate the few `@MockitoSettings(strictness = LENIENT)` wiki tests once mocks are tightened.
9. Confirm the `build-jvm.yml` JVM image still builds in CI (now uses temurin JDK 25, not GraalVM).
10. Consider deleting `MIGRATION_REPORT.md` (this file) once it has served its purpose, or keep it in
    `docs/` as the architecture record.

**Known intentional leftovers (no action required):**
- A handful of test files in `application-api/src/test/java` remain Java (cache/webhook integration
  tests) — they compile and pass against the Kotlin classes; convert to Kotlin only if you want a
  fully‑Kotlin test tree.
- `@file:Suppress("ktlint:standard:function-naming")` on Spring Data repos with `_` traversal, and
  `@file:Suppress("ktlint:standard:max-line-length")` on a few test files with long Korean
  fixtures — intentional.

---

## 8. Risk register

| Risk | Mitigation |
|---|---|
| Subtle behavior drift in the hand‑rewritten wiki RAG/OpenAI path | Ported unit tests pass; run `ragEval` + SSE smoke test (item 2–3). |
| JWT/security semantics changed by the rewrite | Smoke‑test login/refresh (item 1); nullability matched to prior platform behavior. |
| Redis cached entries from before deploy | Same Jackson typing + same DTO FQNs → old entries deserialize; `StartupCacheEvictor` flushes on boot. |
| Boundary regressions over time | Enforced by Gradle deps today; add an ArchUnit/Konsist guard (item 6). |
