---
phase: 06-i-want-to-improve-our-wikichat-and-retreval-service-controlloer-for-better-quillity-for-users-while-chatting
plan: 03
subsystem: api
tags: [wiki-chat, retrieval, reranking, pgvector, testing]
requires:
  - phase: 06-01
    provides: internal wiki chat DTO seams for retrieval and chat quality work
provides:
  - structured wiki retrieval context for downstream chat orchestration
  - section-diverse reranked chunk assembly from pgvector candidates
  - weak-grounding fallback with suggested next questions
affects: [wiki-chat, retrieval, controller]
tech-stack:
  added: []
  patterns: [typed retrieval context, app-layer rerank after vector fetch, weak-grounding fallback suggestions]
key-files:
  created: []
  modified:
    - src/main/java/kr/devport/api/domain/wiki/service/WikiRetrievalService.java
    - src/main/java/kr/devport/api/domain/wiki/service/WikiChatService.java
    - src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceTest.java
    - src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceFallbackTest.java
key-decisions:
  - "Fetch 8 pgvector candidates, then greedily rerank in application code with heading overlap and section diversity before building prompt context."
  - "Treat retrieval outages or empty vector matches as weak-grounding fallbacks that still return chunk-backed context plus 2-3 recovery questions."
patterns-established:
  - "Retrieval returns WikiRetrievalContext instead of a raw concatenated string so later chat layers can react to grounding strength."
  - "Weak-grounding responses stay action-oriented by pairing surviving repo chunks with suggested follow-up questions."
requirements-completed: [CHAT-QUAL-04, CHAT-QUAL-06]
duration: 2 min
completed: 2026-03-08
---

# Phase 6 Plan 3: Retrieval Quality Summary

**Structured wiki retrieval now returns reranked diverse chunk context with weak-grounding recovery questions instead of collapsing to empty prompt input.**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-08T12:12:46+09:00
- **Completed:** 2026-03-08T12:15:22+09:00
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments
- Replaced raw top-k concatenation with `WikiRetrievalContext` that carries grounding strength and selected chunks.
- Added deterministic app-layer reranking so duplicate-heavy vector results no longer crowd out other relevant sections.
- Added weak-grounding fallback behavior with 2-3 suggested next questions when vector retrieval partially fails.

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace raw top-k concatenation with structured reranked retrieval** - `6282162` (test), `211cc25` (feat)

**Plan metadata:** pending

_Note: TDD tasks may have multiple commits (test -> feat -> refactor)_

## Files Created/Modified
- `src/main/java/kr/devport/api/domain/wiki/service/WikiRetrievalService.java` - Returns structured retrieval context, reranks candidates, and assembles weak-grounding fallbacks.
- `src/main/java/kr/devport/api/domain/wiki/service/WikiChatService.java` - Consumes grounded context from the typed retrieval result.
- `src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceTest.java` - Covers reranking diversity and weak-grounding fallback behavior.
- `src/test/java/kr/devport/api/domain/wiki/service/WikiRetrievalServiceFallbackTest.java` - Enables regression seams for fallback suggestions and diverse chunk expectations.

## Decisions Made
- Fetch 8 candidates from pgvector, then rerank in Java so section diversity and heading overlap influence final grounding selection.
- Keep no-wiki-chunks-for-project as an error, but convert partial retrieval failures into weak-grounding contexts with recovery prompts.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `WikiChatService` can now build on typed retrieval metadata instead of a raw string.
- Phase 06-04 can use `weakGrounding` and `suggestedNextQuestions` to improve Korean answer orchestration without changing the compact controller contract.

---
*Phase: 06-i-want-to-improve-our-wikichat-and-retreval-service-controlloer-for-better-quillity-for-users-while-chatting*
*Completed: 2026-03-08*

## Self-Check: PASSED
