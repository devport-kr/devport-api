---
phase: 05-new-phase1
plan: "04"
subsystem: wiki-chat
tags: [openai, chat, rag, session-management]

# Dependency graph
requires:
  - phase: 05-02
    provides: wiki snapshot foundation with Core-6 sections
  - phase: 05-03
    provides: API read endpoints with section visibility and security
provides:
  - Wiki chat API with high-precision repo Q&A
  - Session-scoped memory with TTL expiration
  - Uncertainty clarification behavior
  - On-demand citation expansion
affects: [05-05-frontend-integration, future-chat-enhancements]

# Tech tracking
tech-stack:
  added: []
  patterns: [RAG-with-grounded-context, uncertainty-handling, short-lived-session-memory]

key-files:
  created: 
    - src/main/java/kr/devport/api/domain/wiki/service/WikiRetrievalService.java
    - src/main/java/kr/devport/api/domain/wiki/service/WikiChatService.java
    - src/main/java/kr/devport/api/domain/wiki/store/WikiChatSessionStore.java
    - src/main/java/kr/devport/api/domain/wiki/controller/WikiChatController.java
    - src/main/java/kr/devport/api/domain/wiki/dto/request/WikiChatRequest.java
    - src/main/java/kr/devport/api/domain/wiki/dto/response/WikiChatResponse.java
  modified: []

key-decisions:
  - "WikiRetrievalService prioritizes architecture > how > what sections for context assembly"
  - "Session memory kept in-memory with ConcurrentHashMap instead of Redis for simplicity"
  - "Uncertainty detection uses simple heuristics (question marks + uncertainty phrases) rather than LLM confidence scores"
  - "Citations are optional via includeCitations flag to keep default responses compact"

patterns-established:
  - "Pattern 1: Retrieval service separates repo context from ecosystem context for bounded token windows"
  - "Pattern 2: Session store auto-expires old sessions (30-min TTL) and prunes old turns (max 10 per session)"
  - "Pattern 3: Chat controller returns isClarification flag for frontend to handle uncertainty UX"

# Metrics
duration: 6min
completed: 2026-02-15
---

# Phase 05 Plan 04: Wiki Chat API Summary

**High-precision repo Q&A with grounded retrieval, session-scoped memory, uncertainty clarification, and on-demand citations using OpenAI GPT-4o-mini**

## Performance

- **Duration:** 6 min
- **Started:** 2026-02-15T03:32:10Z
- **Completed:** 2026-02-15T03:38:29Z
- **Tasks:** 3
- **Files modified:** 10 (6 created, 0 modified, 4 test files)

## Accomplishments
- Implemented retrieval service combining repo wiki snapshot context with constrained ecosystem context
- Added session-scoped memory with TTL expiration (30 min) and automatic turn pruning (max 10 turns)
- Created uncertainty handling that returns clarifying questions when confidence is low
- Exposed chat API endpoint with compact payloads and optional citation expansion

## Task Commits

Each task was committed atomically:

1. **Task 1: Build retrieval service** - `01e3d45` (feat)
2. **Task 2: Add session memory and uncertainty handling** - `d536f82` (feat)
3. **Task 3: Expose chat API** - `2812dd9` (feat)

## Files Created/Modified
- `WikiRetrievalService.java` - Fuses repo and ecosystem context with deterministic source selection
- `WikiChatService.java` - Generates chat responses with uncertainty handling and session memory
- `WikiChatSessionStore.java` - TTL-based in-memory session storage with automatic expiration
- `WikiChatController.java` - REST endpoint for chat with on-demand citation expansion
- `WikiChatRequest.java` - Request DTO with question, sessionId, includeCitations flag
- `WikiChatResponse.java` - Response DTO with answer, isClarification flag, optional citations

## Decisions Made
- **Context prioritization:** Architecture sections prioritized over how/what for technical Q&A relevance
- **Session storage:** Used ConcurrentHashMap instead of Redis for simplicity (no cross-instance coordination needed)
- **Uncertainty detection:** Simple heuristic-based approach (question marks + uncertainty phrases) rather than parsing LLM confidence scores
- **Citation model:** Optional flag-based inclusion to keep default responses compact for right-rail UX

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## Next Phase Readiness
- Chat backend complete, ready for frontend integration (05-05)
- On-demand citations infrastructure in place but citations currently return empty list (future enhancement)
- Session memory works for single-instance deployment; multi-instance would need Redis or sticky sessions

---
*Phase: 05-new-phase1*
*Completed: 2026-02-15*
