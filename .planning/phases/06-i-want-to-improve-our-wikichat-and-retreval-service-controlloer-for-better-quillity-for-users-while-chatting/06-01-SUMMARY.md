---
phase: 06-i-want-to-improve-our-wikichat-and-retreval-service-controlloer-for-better-quillity-for-users-while-chatting
plan: "01"
subsystem: api
tags: [wiki-chat, retrieval, regression-tests, spring-boot]
requires: []
provides:
  - Internal wiki chat DTO seams for retrieval and orchestration refactors.
  - Wave 0 regression harnesses for session, retrieval fallback, chat quality, and compact controller behavior.
affects: [wiki-chat, retrieval, controller, testing]
tech-stack:
  added: []
  patterns: [internal service DTO seams, disabled-forward regression scaffolds, compact controller contract assertions]
key-files:
  created:
    - src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiRetrievedChunk.java
    - src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiRetrievalContext.java
    - src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiChatResult.java
    - src/test/java/kr/devport/api/domain/wiki/store/WikiChatSessionStoreTest.java
    - src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceFallbackTest.java
    - src/test/java/kr/devport/api/domain/wiki/service/WikiChatServiceQualityTest.java
    - src/test/java/kr/devport/api/domain/wiki/controller/WikiChatControllerWebMvcTest.java
  modified:
    - src/main/java/kr/devport/api/domain/wiki/dto/response/WikiChatResponse.java
key-decisions:
  - "Create phase-local internal DTO records under dto/internal so later plans can refactor retrieval and chat orchestration without changing the public controller payload."
  - "Use executable scaffold tests with selectively disabled future assertions to lock file locations and behavior names before service refactors land."
  - "Annotate WikiChatResponse.isClarification with JsonProperty so the compact API contract serializes the expected field name."
patterns-established:
  - "Internal wiki chat contracts live under dto/internal until behavior plans intentionally expose new public fields."
  - "Phase 6 quality harnesses keep future-facing scenarios disabled but compilable, with active smoke assertions that verify seams and payload shape today."
requirements-completed: [CHAT-QUAL-01, CHAT-QUAL-02, CHAT-QUAL-03, CHAT-QUAL-04, CHAT-QUAL-05]
duration: 5 min
completed: 2026-03-08
---

# Phase 6 Plan 1: Internal wiki chat contracts and regression scaffold summary

**Internal wiki chat DTO seams plus executable Wave 0 regression harnesses for session safety, retrieval fallback, service quality, and compact controller payloads.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-08T03:00:00Z
- **Completed:** 2026-03-08T03:05:31Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- Added `WikiRetrievedChunk`, `WikiRetrievalContext`, and `WikiChatResult` as stable internal contracts for downstream Phase 6 plans.
- Created the four missing Wave 0 regression classes referenced by `06-VALIDATION.md` with active smoke assertions and disabled future-behavior placeholders.
- Locked the compact controller JSON field name to `isClarification` so new MVC assertions match the intended API contract.

## Task Commits

Each task was committed atomically:

1. **Task 1: Write internal retrieval and chat-result contracts** - `7441038` (feat)
2. **Task 2: Create the missing Phase 6 regression scaffold classes** - `498d231` (test)

## Files Created/Modified
- `src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiRetrievedChunk.java` - Internal retrieved-chunk record for later reranking and provenance work.
- `src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiRetrievalContext.java` - Internal retrieval result seam with grounding strength and next-question hooks.
- `src/main/java/kr/devport/api/domain/wiki/dto/internal/WikiChatResult.java` - Internal chat-result seam with clarification, continuity, and reset flags.
- `src/test/java/kr/devport/api/domain/wiki/store/WikiChatSessionStoreTest.java` - Session-store regression harness for recent-turn retention, pruning, and future reset behavior.
- `src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceFallbackTest.java` - Retrieval fallback harness for weak-grounding seams and future rerank/fallback scenarios.
- `src/test/java/kr/devport/api/domain/wiki/service/WikiChatServiceQualityTest.java` - Combined broad-answer-first and ambiguity clarification scaffold for Phase 6 service quality work.
- `src/test/java/kr/devport/api/domain/wiki/controller/WikiChatControllerWebMvcTest.java` - MVC contract scaffold for compact payload and validation behavior.
- `src/main/java/kr/devport/api/domain/wiki/dto/response/WikiChatResponse.java` - Explicit JSON property mapping for `isClarification`.

## Decisions Made
- Internal service-layer contracts stay separate from `WikiChatResponse` so later plans can evolve orchestration without prematurely expanding the public API.
- `WikiChatServiceQualityTest` owns both broad-question and ambiguity-clarification scaffolding, matching the validation map instead of splitting behaviors across extra files.
- Disabled assertions are kept executable and named after locked requirements so future plans can enable them in place.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed compact chat JSON field naming for `isClarification`**
- **Found during:** Task 2 (Create the missing Phase 6 regression scaffold classes)
- **Issue:** MVC scaffold assertions exposed that Jackson serialized the boolean field as `clarification`, which did not match the intended compact contract field name `isClarification`.
- **Fix:** Added `@JsonProperty("isClarification")` on `WikiChatResponse.isClarification`.
- **Files modified:** `src/main/java/kr/devport/api/domain/wiki/dto/response/WikiChatResponse.java`
- **Verification:** `./gradlew test --tests "kr.devport.api.domain.wiki.store.WikiChatSessionStoreTest" --tests "kr.devport.api.domain.wiki.service.WikiRetrievalServiceFallbackTest" --tests "kr.devport.api.domain.wiki.service.WikiChatServiceQualityTest" --tests "kr.devport.api.domain.wiki.controller.WikiChatControllerWebMvcTest"`
- **Committed in:** `498d231`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** The fix stayed within the compact response contract and made the new regression scaffold reflect the intended API behavior.

## Issues Encountered
- None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 6 now has concrete internal seams and named regression targets for memory, retrieval fallback, and controller behavior.
- `06-02` can focus on session reset and recent-turn selection without re-deciding test locations or DTO field names.

## Self-Check: PASSED
- Verified `06-01-SUMMARY.md` exists on disk.
- Verified task commits `7441038` and `498d231` exist in git history.

---
*Phase: 06-i-want-to-improve-our-wikichat-and-retreval-service-controlloer-for-better-quillity-for-users-while-chatting*
*Completed: 2026-03-08*
