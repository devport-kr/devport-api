# wiki — strict-hexagonal refactor spec (handoff)

5/6 domains are done (llm, auth, article, port, mypage) — each committed, compiling, ktlint-clean.
wiki is the last and largest (~3,970 LOC). This is the fully-worked-out plan; nothing here needs
re-deriving. Follow the exact same pattern as the committed domains (e.g. `port`, commit `598ba8d`).

## Key findings (already analysed)

- **wiki can drop `auth` entirely.** `User?` only flows in to set `session.user` and to query
  sessions by user. Thread **`userId: Long?`** instead; no `UserDirectory` needed.
  - `WikiChatApplicationService.resolveUser(...)` → just apply rate limits and return `userId`.
  - `WikiChatSessionPersistenceService`: drop `UserRepository`/`resolveUser`; query by `userId`.
- **wiki needs a `port` inbound port.** Only two call sites:
  `WikiService` → `projectRepository.findAll(Sort by stars desc)`,
  `WikiAdminProjectQueryService` → `projectRepository.findAllForWikiAdmin()`.
  Add `ProjectDirectory` (inbound port) to **`port:infrastructure`** + impl in `port:service`
  (mirror `ArticleDirectory`/`UserDirectory`). Expose: `listAllByStarsDesc(): List<ProjectView>`
  and `listForWikiAdmin(): List<ProjectView>` where `ProjectView` is a pure model in `port:model`
  carrying the fields `WikiService`/`WikiAdminProjectQueryService` map (name, fullName, stars,
  language, externalId, …). Check those two mappers for the exact fields.

## Module layout to create

```
wiki:model            WikiChatSession.user(@ManyToOne) → userId(Long?)  (drop auth:entity.User import)
wiki:infrastructure   (NEW, type=kotlin-boot) ports:
                        - persistence: WikiChatSessionRepository, WikiChatMessageRepository,
                          WikiSectionChunkRepository (incl. the pgvector custom methods + ScoredChunkRow)
                        - external: ChatPort, EmbeddingPort, WikiChatSessionStore (port),
                          RateLimitCounter; + domain types ChatMessage(role,content), ChatRole,
                          JsonSchemaSpec(name, schema: Map<String,Any>), and ChatTurn (moved out of
                          WikiChatSessionStore — referenced by persistence + chat services)
wiki:repository-jpa   *JpaRepository + @Repository adapters; keep WikiSectionChunkRepositoryImpl
                        (JdbcTemplate pgvector). Session repo: findByUser(User,…) → findByUserId(Long,…),
                        drop @EntityGraph(["user"]).
wiki:adapter-openai   (NEW) OpenAiChatAdapter : ChatPort, OpenAiEmbeddingAdapter : EmbeddingPort
wiki:adapter-redis    (NEW) WikiChatSessionRedisStore : WikiChatSessionStore (move the 252-line store
                        here verbatim, keep key prefixes + map serialization), RedisRateLimitCounter
wiki:service          depends on infrastructure (+ port:infrastructure) only; no openai/redis/auth/web
wiki:api              unchanged controllers, but pass userId (CustomUserDetails.id) not User
```

## Port designs (from the actual call sites)

```kotlin
// EmbeddingPort — WikiRetrievalService.embedText + WikiGlobalRetrievalService.embedText
interface EmbeddingPort { fun embed(text: String): FloatArray }   // model "text-embedding-3-small"

// ChatPort — covers all 4 chat call sites (2 non-streaming-structured, 2 streaming, + reranker + title)
data class ChatMessage(val role: ChatRole, val content: String)
enum class ChatRole { SYSTEM, USER, ASSISTANT }
data class JsonSchemaSpec(val name: String, val schema: Map<String, Any>)   // structured output

interface ChatPort {
    fun complete(
        model: String,                      // pass ChatModel.GPT_5_MINI / GPT_4O_MINI as string
        messages: List<ChatMessage>,
        maxCompletionTokens: Long? = null,  // title uses 30
        jsonSchema: JsonSchemaSpec? = null, // reranker + both non-stream chats use structured output
    ): String                               // returns choices[0].message.content (orElse "")

    fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,          // adapter iterates createStreaming, forwards delta().content()
    )
}
```

Per service, the OpenAI edit is **localized** (no rewrite of prompt/parse/persist logic):
1. `buildMessages(...)` returns `List<ChatMessage>` instead of `List<ChatCompletionMessageParam>`
   (map system→SYSTEM, user→USER).
2. `buildResponseFormat()` returns a `JsonSchemaSpec` with a plain Kotlin `Map` (the schema is already
   essentially a map; drop the `ResponseFormatJsonSchema`/`JsonValue` builders — those move to the adapter).
3. Replace `openAIClient.chat().completions().create(...)` with `chatPort.complete(model, msgs, …)`
   and `.createStreaming(...){ … }` with `chatPort.stream(model, msgs) { token -> tokenConsumer.accept(token); accumulated.append(token) }`.
4. Replace `openAIClient.embeddings().create(...)` with `embeddingPort.embed(text)`.

The adapter (`OpenAiChatAdapter`) owns: ChatMessage→`ChatCompletionMessageParam`, JsonSchemaSpec→
`ResponseFormatJsonSchema`, `maxCompletionTokens`, and the `createStreaming().use { stream().use { … } }`
loop calling `onToken` for each `choice.delta().content()`. Keep `OpenAIClient` bean in application-api.

## Redis (wiki:adapter-redis)

- `WikiChatSessionStore` becomes a **port** (interface) in `wiki:infrastructure` with the public
  methods the services call (`saveTurn`, `loadRecentTurns`, `hasActiveSession`, `clear`, …) + the
  `ChatTurn`/`ChatSession` data classes moved to infrastructure. Move the existing impl to
  `WikiChatSessionRedisStore` in the adapter **verbatim** (preserve `KEY_PREFIX` + map serialization
  so live sessions survive a deploy).
- `RateLimitCounter` port: `fun hit(key: String, window: Duration): Long?` (increment + set TTL on
  first hit). `WikiAnonRateLimiter`/`WikiChatRateLimiter` keep their policy and call the port; the
  Redis `increment`/`expire` moves to `RedisRateLimitCounter` in the adapter. (These two limiters are
  currently `@Component` in wiki:service — after this they hold only policy and use the port.)

## Cross-domain `@ManyToOne` removal

- `WikiChatSession.user: User?` → `@Column(name="user_id") var userId: Long? = null` (nullable — anon
  sessions). `idx_wiki_chat_sessions_user_id` already on `user_id`. **No schema change.**
- Session repo derived queries: `findByUserOrderByLastMessageAtDesc(user, …)` →
  `findByUserIdOrderByLastMessageAtDesc(userId, …)`, etc. Auth checks
  `session.user?.id != userId` → `session.userId != userId`.

## Then: composition + guard + tests (Workstreams done last)

- **application-api**: add `wiki:infrastructure`, `wiki:adapter-openai`, `wiki:adapter-redis` deps
  (auth/article/port/mypage adapter+infra deps already added). Add `port:` `ProjectDirectory` is in
  `port:service` (already a dep). Verify `@EntityScan`/`@EnableJpaRepositories` still cover all
  `kr.devport.api...entity`/`...repository` packages (unchanged — entities stayed in `model`).
- **Konsist guard** (Workstream A): add `konsist` to `libs.versions.toml` + `application-api`
  testImplementation; `ArchitectureBoundaryTest` rules (pragmatic variant — entities stay in `model`):
  service files import no `..repository..`, no `org.springframework.data.jpa.repository.JpaRepository`,
  no `com.openai..`/`org.springframework.data.redis..`/`org.springframework.web..`/`jakarta.servlet..`;
  api files import no `..repository..`; model files import no other-domain `kr.devport.api.domain.<other>`.
- **Tests**: ~30 files in `application-api/src/test` reference old types (`WikiChatService` ctor with
  `OpenAIClient`, repository classes, `comment.user`, `session.user`, etc.). Update to the new ports /
  `userId`. The `contextLoads` `@SpringBootTest` is the integration gate.

## Verification (green gate)

`./gradlew build` green across all modules · `ArchitectureBoundaryTest` green · `contextLoads` boots ·
`./gradlew :<d>:service:dependencies` shows no repository-jpa/adapter/openai/redis/web on any service ·
smoke: OAuth login+JWT, wiki SSE streaming, crawler webhook, Redis cache/session, `:application-api:ragEval`.
