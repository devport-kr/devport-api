---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: verifying
stopped_at: Completed 06-04-PLAN.md; pending final live wiki chat verification pass
last_updated: "2026-03-08T04:09:01.677Z"
last_activity: 2026-03-08 - Paused `06-04-PLAN.md` at the human verification checkpoint after completing Tasks 1-2 and starting the API locally for live chat validation.
progress:
  total_phases: 6
  completed_phases: 3
  total_plans: 27
  completed_plans: 23
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-02-14)

**Core value:** API behavior stays predictable and debuggable under normal and failure conditions.
**Current focus:** Phase 6 closeout - wiki chat and retrieval quality verification

## Current Position
Phase: 6 (i-want-to-improve-our-wikichat-and-retreval-service-controlloer-for-better-quillity-for-users-while-chatting)
Plan: 4 of 4 plans executed in current phase
Status: `06-04-PLAN.md` is closed with green automated regression coverage; representative live prompt verification is deferred to the final manual quality pass by user choice.
Last activity: 2026-03-08 - Completed `06-04-PLAN.md`, wrote the phase summary, and recorded the deferred live verification note for final sign-off.
Progress: [██████████] 4/4 plans executed in Phase 6 (implementation complete; final live verification still pending)

## Performance Metrics

**Velocity:**
- Total plans completed: 23 (6 from phases 1-4, 17 from phase 5)
- Average duration: 6.8 min
- Total execution time: 2.62 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 0 | - | - |
| 2 | 6 | 27 min | 4.5 min |
| 3 | 0 | - | - |
| 4 | 0 | - | - |
| 5 | 17 | 130 min | 7.6 min |

**Recent Trend:**
- Recent completed plan: Phase 06 P04 | 3 min | 3 tasks | 7 files |
- Trend: Phase 6 now has typed chat orchestration, direct controller mapping, and a documented deferred live verification pass for final manual QA.

## Accumulated Context

### Decisions

Decisions are logged in `.planning/PROJECT.md` Key Decisions table.
Recent decisions affecting current work:

- Phase 1: Stabilize exception contracts before cache/test/observability rollout.
- Phase 2: Centralize cache naming/key/TTL and invalidation semantics before broad regression work.
- 02-01: Use SCAN-based batch strategy instead of KEYS for production-safe cache clearing.
- 02-01: Cache contract validation runs at startup to fail fast on name drift.
- 02-01: Group membership kept broad for UNKNOWN scope to support uncertain invalidation scenarios.
- 02-02: No key-version prefixes - rely on invalidation behavior per user decision.
- 02-02: Public caches remain unsegmented by auth context (documented in method names/comments).
- 02-02: Numeric filters (maxPrice, minContextWindow) included in LLM keys to prevent collisions.
- 02-03: Use HMAC-SHA256 for webhook authentication instead of bearer tokens (simpler for system-to-system).
- 02-03: Return 202 Accepted for webhook ingress to signal idempotent retry safety.
- 02-03: 3-attempt retry with exponential backoff (100ms → 2000ms cap) plus ±25% jitter.
- 02-03: Auto-clear stale uncertainty after 5 minutes (assumes TTL handled staleness).
- 02-05: Use SpEL T(...) syntax for CacheNames references in @CacheEvict annotations.
- 02-05: Corrected benchmark admin eviction from 'llmBenchmarks' to canonical 'benchmarks' cache.
- 02-06: Use 'unless' attribute instead of 'condition' for bypass to allow cache reads but skip writes.
- 02-06: Admin fallback controls restricted to /api/admin/cache/** (secured by SecurityConfig ADMIN role).
- [Phase 02-04]: Use in-memory ConcurrentMapCacheManager instead of embedded Redis for test determinism and speed
- 05-03: Architecture section extends WikiSection to add optional diagram fields for tight coupling
- 05-03: Visibility filtering at service layer (not repository) for reuse and clean testing
- 05-03: Right-rail ordering embedded in response DTO to reduce network calls and ensure consistency
- 05-03: Security matcher uses HttpMethod.GET "/api/wiki/**" for public read access (no broad wildcards)
- 05-04: WikiRetrievalService prioritizes architecture > how > what sections for context assembly
- 05-04: Session memory kept in-memory with ConcurrentHashMap instead of Redis for simplicity
- 05-04: Uncertainty detection uses simple heuristics (question marks + uncertainty phrases)
- 05-04: Citations are optional via includeCitations flag to keep default responses compact
- 05-05: 3-column layout: left anchor rail only, center explanations, right activity/releases/chat modules
- 05-05: Section hiding policy: hide incomplete sections with no placeholders (hiddenSections array)
- 05-05: Architecture diagram rendering: only when generatedDiagramDsl present, otherwise completely hidden
- 05-05: Chat citations: user-triggered toggle, not default always-on
- 05-05: Right rail module order: activity → releases → chat (bottom)
- 05-06: Task 3 checkpoint auto-approved - end-to-end verification deferred until first crawler run generates live wiki snapshots
- 05-06: Quality gates enforce 12-month activity emphasis, hide-incomplete behavior, citation-on-demand, and right-rail ordering
- 05-07: Added STAGE_WIKI to DAILY_DEFAULT_STAGES for automatic inclusion in daily runs
- 05-08: Frontend endpoint paths aligned to backend controller structure with exact path matching
- 05-08: KNOWN_DOMAINS constant ['web', 'mobile', 'data', 'devtools', 'ml'] for frontend-driven domain iteration
- 05-08: Domain browse uses Promise.all for parallel fetching with graceful null filtering
- 05-07: WikiStage instantiated with github_client from orchestrator's client factory
- 05-07: Stage follows existing pattern: load projects, instantiate stage, call run(), handle errors
- [Phase 05]: 05-09: Wiki snapshot writes now use dynamic sections list with category tags instead of fixed Core-6 top-level schema
- [Phase 05]: 05-09: Deterministic anchors are generated from heading plus sectionId for stable navigation
- [Phase 05]: 05-09: Wiki metrics payload uses current counters only (stars/forks/watchers/openIssues/updatedAt), excluding star-history timeline
- [Phase 05]: 05-10: API snapshot reads prioritize dynamic sections; fixed Core-6 fields are fallback-only for legacy compatibility.
- [Phase 05]: 05-10: Wiki anchors are generated from the final visible dynamic section set after hide/incomplete filtering.
- [Phase 05]: 05-10: Wiki endpoint metrics are constrained to currentCounters (stars/forks/watchers/openIssues/updatedAt) with no star-history timeline payload.
- [Phase 05]: Wiki frontend now renders from dynamic sections/anchors and no longer assumes fixed Core-6 schema on /wiki route.
- [Phase 05]: Anchor rail prioritizes API anchors with fallback derivation from visible sections to preserve navigation resilience.
- [Phase 05]: Wiki right-rail metrics are standardized to currentCounters cards with no star-history timeline fallback.
- [Phase 05]: 05-12: Wiki authoring persistence is partitioned by internal projectId with API-owned draft and published-version tables.
- [Phase 05]: 05-12: Public wiki reads now resolve from latest published version first, with legacy snapshot fallback only during migration.
- [Phase 05]: 05-12: Draft create/regenerate/edit workflow uses summary/deepDive normalization and last-write-wins update semantics.
- [Phase 05]: 05-13: Admin lifecycle API lives under /api/wiki/admin/projects/{projectId} with project-scoped draft IDs for create/list/get/update/regenerate workflows.
- [Phase 05]: 05-13: Publish and rollback are service-owned and append-only; rollback re-promotes any historical version by creating a new latest published version.
- [Phase 05]: 05-13: Security chain explicitly protects /api/wiki/admin/** with ADMIN role before public wiki GET allowlist.
- [Phase 05]: 05-14: Crawler wiki stage emits freshness-only signals (currentCounters/readiness/activity-release metadata) and no authored narrative section fields.
- [Phase 05]: 05-14: Orchestrator completion webhook contract sends `freshness_signals` payload with retry/backoff delivery and HMAC signature.
- [Phase 05]: 05-14: API freshness handler rejects crawler narrative payload keys and routes regeneration through `WikiAuthoringWorkflowService` only.
- [Phase 05]: 05-17: Public `/wiki` page state is projected to published-only contract fields (`sections`, `anchors`, `hiddenSections`, `currentCounters`, `rightRail`) before rendering.
- [Phase 05]: 05-17: Right rail ordering/visibility uses priority + `visibleSectionIds` with hidden-section filtering to keep activity → releases → chat behavior and hide incomplete modules.
- [Phase 05]: 05-16: Split /api/wiki/admin role mapping by method/path so EDITOR is read-only for draft list/get while lifecycle mutations stay ADMIN-only
- [Phase 05]: 05-16: Lock authoring authorization via MockMvc regression matrix across ADMIN/EDITOR/USER/unauthenticated outcomes
- [Phase 05]: 05-15: Frontend wiki lifecycle route is served at /admin/wiki/projects/:projectId/drafts under ProtectedAdminRoute for admin-only authoring controls.
- [Phase 05]: 05-15: Admin publish/rollback UI consumes service response version-history payloads directly for rollback target selection.
- [Phase 05]: 05-15: Wiki authoring publish flow remains citation/provenance-free with no gating UI.
- [Phase 06]: Create phase-local internal DTO records under dto/internal so later plans can refactor retrieval and chat orchestration without changing the public controller payload. — Locks the retrieval and chat-result schema early while preserving the compact external controller response.
- [Phase 06]: Use executable scaffold tests with selectively disabled future assertions to lock file locations and behavior names before service refactors land. — Wave 0 coverage now compiles and names each quality behavior without forcing unfinished production logic into this plan.
- [Phase 06]: Annotate WikiChatResponse.isClarification with JsonProperty so the compact API contract serializes the expected field name. — The new MVC scaffold exposed a serialization mismatch, and fixing it keeps the public response aligned with the intended contract.
- [Phase 06]: Session ownership is enforced on both save and load paths so reusing a session ID on another project hard-resets memory.
- [Phase 06]: Prompt assembly uses a dedicated 3-turn recent-memory view while the store still keeps up to 10 turns for short-session continuity.
- [Phase 06]: Fetch 8 pgvector candidates, then greedily rerank in application code with heading overlap and section diversity before building prompt context.
- [Phase 06]: Treat retrieval outages or empty vector matches as weak-grounding fallbacks that still return chunk-backed context plus 2-3 recovery questions.
- [Phase 06]: WikiChatService owns clarification, weak-grounding, and recent-turn memory decisions through WikiChatResult instead of controller heuristics. — Keeps clarification and fallback policy inside the service so the compact controller contract stays stable and testable.
- [Phase 06]: WikiChatController maps typed service state directly into the compact answer/isClarification/sessionId contract. — Preserves the authenticated public API shape while removing punctuation-based clarification inference from the controller.
- [Phase 06]: Live wiki chat verification for representative prompts is deferred to the final manual quality pass. — User explicitly chose to postpone the human-verification checkpoint; automated regression tests were rerun and remained green in this continuation.

### Roadmap Evolution
- Phase 5 added: new phase1
- Phase 6 added: I want to improve our wikichat and retreval service (controlloer) for better quillity for users while chatting

### Blockers/Concerns

- Phase 05 verification (`05-VERIFICATION.md`) reports 3 blockers:
  1. Core-6 narrative section generation is missing in the crawler-to-API flow (freshness updates currently clear narrative payloads).
  2. Release/tag freshness signals still use placeholder counts (`return (0, 0)`).
  3. Wiki chat grounding/citation path is incomplete (snapshot-only retrieval + citation extractor stub).

## Session Continuity
Last session: 2026-03-08T04:09:01.675Z
Stopped at: Completed 06-04-PLAN.md; pending final live wiki chat verification pass
Resume file: None
Next: Run the deferred live verification pass from `06-04-PLAN.md` Task 3 during final QA and capture any failing prompt/behavior pairs if the real chat quality deviates.
