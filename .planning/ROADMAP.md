# Roadmap: DevPort API Stabilization Milestone

## Overview

This roadmap delivers stabilization in dependency order: first make error behavior deterministic, then make cache behavior deterministic, then lock both with regression coverage, and finally add the observability and auth hardening needed for safer ongoing maintenance. The structure follows v1 requirements directly and keeps scope focused on reliability, security hardening, and maintainability without feature expansion.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Exception Contract Baseline** - Domain failures return explicit, predictable API errors.
- [x] **Phase 2: Cache Correctness Hardening** - Read/write cache behavior becomes deterministic and safe.
- [ ] **Phase 3: Regression Safety Net** - Integration tests prevent exception/cache regressions.
- [ ] **Phase 4: Observability and Security Hardening** - Failure visibility and auth-edge protections are verifiable.

## Phase Details

### Phase 1: Exception Contract Baseline
**Goal**: API clients receive deterministic, structured responses for expected domain failures while unknown failures stay sanitized.
**Depends on**: Nothing (first phase)
**Requirements**: EXC-01, EXC-02, EXC-03, EXC-04
**Success Criteria** (what must be TRUE):
  1. API clients get mapped 4xx/5xx statuses for representative domain failures instead of broad runtime-error behavior.
  2. All mapped domain failures return the stable payload fields `type`, `title`, `status`, `detail`, and `instance`.
  3. Repeating the same domain failure on scoped endpoints yields the same status and payload shape each time.
  4. Unknown/unhandled failures return sanitized server errors with no sensitive internals exposed.
**Plans**: 4 plans

Plans:
- [ ] 01-01: TBD

### Phase 2: Cache Correctness Hardening
**Goal**: Cached reads stay correct after mutations through consistent names, keys, TTL ownership, and safe invalidation.
**Depends on**: Phase 1
**Requirements**: CACH-01, CACH-02, CACH-03, CACH-04, CACH-05
**Success Criteria** (what must be TRUE):
  1. After create/update/delete on scoped resources, subsequent reads return fresh data for affected endpoints.
  2. Cache names and key composition behave deterministically across read/write paths and avoid cross-query collisions.
  3. Each critical cache has explicit TTL behavior that is visible in code/config and consistent at runtime.
  4. Cache clearing/invalidation uses production-safe patterns and is verified by integration coverage.
**Plans**: 6 plans

Plans:
- [x] 02-01-PLAN.md - Centralize cache names/groups/TTL ownership and safe Redis cache clear behavior.
- [x] 02-02-PLAN.md - Canonicalize read-path cache keys and wire deterministic keying across cached endpoints.
- [x] 02-03-PLAN.md - Add crawler webhook ingress plus scoped invalidation, retry/backoff, and uncertainty-state tracking.
- [x] 02-04-PLAN.md - Add integration coverage for invalidation correctness, retry windows, and fallback behavior.
- [x] 02-05-PLAN.md - Rewire admin write-path cache eviction to centralized cache names.
- [x] 02-06-PLAN.md - Wire uncertainty-aware cache bypass and admin/internal fallback control endpoints.

### Phase 3: Regression Safety Net
**Goal**: Automated tests reliably detect regressions in error contracts and cache invalidation behavior before release.
**Depends on**: Phase 1, Phase 2
**Requirements**: TEST-01, TEST-02, TEST-03
**Success Criteria** (what must be TRUE):
  1. Integration tests fail if exception-to-status mapping regresses for representative auth/article/project failures.
  2. Integration tests fail if mutations do not invalidate cached reads for affected endpoints.
  3. At least one rollback/failure-path test proves stale cached data is not served in the previously failing scenario.
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

### Phase 4: Observability and Security Hardening
**Goal**: Maintainers can quickly diagnose scoped failures and auth-sensitive endpoints enforce explicit guardrails.
**Depends on**: Phase 1, Phase 2, Phase 3
**Requirements**: OBSV-01, OBSV-02, SECU-01
**Success Criteria** (what must be TRUE):
  1. Scoped failure logs include a request correlation identifier and exception class metadata per request.
  2. Metrics expose exception-class counts plus cache eviction/miss behavior for critical caches.
  3. Auth-sensitive endpoints enforce explicit validation/abuse-resistance checks, with test coverage confirming expected rejection behavior.
**Plans**: TBD

Plans:
- [ ] 04-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Exception Contract Baseline | 0/TBD | Not started | - |
| 2. Cache Correctness Hardening | 6/6 | ✓ Complete | 2026-02-14 |
| 3. Regression Safety Net | 0/TBD | Not started | - |
| 4. Observability and Security Hardening | 0/TBD | Not started | - |
| 5. new phase1 | 17/17 | ✓ Complete | 2026-02-16 |
| 6. Wiki chat and retrieval quality | 4/4 | ✓ Complete | 2026-03-08 |

### Phase 5: new phase1

**Goal:** Deliver a data-first Code Wiki experience for top-star open-source repositories with Core-6 technical sections, near-real-time activity/release freshness, and high-precision repo chat across crawler, API, and frontend.
**Depends on:** Phase 4
**Plans:** 17 plans

Plans:
- [x] 05-01-PLAN.md - Define shared Core-6 wiki snapshot contract and data-readiness foundations across crawler/API/app.
- [x] 05-02-PLAN.md - Implement crawler wiki generation pipeline with adaptive technical explanations and release/tag timeline synthesis.
- [x] 05-03-PLAN.md - Deliver API domain browse and project wiki endpoints with strict hide-incomplete section behavior.
- [x] 05-04-PLAN.md - Add wiki chat API with repo+ecosystem retrieval, uncertainty clarifications, short-lived memory, and on-demand citations.
- [x] 05-05-PLAN.md - Build frontend long-page anchored wiki UX with right-rail activity/releases prioritized above chat.
- [x] 05-06-PLAN.md - Add cross-repo quality gates, launch coverage-threshold reporting, and final human verification.
- [x] 05-07-PLAN.md - Wire WikiStage into orchestrator pipeline for end-to-end execution (gap closure).
- [x] 05-08-PLAN.md - Fix frontend-API endpoint path mismatches for wiki snapshot, chat, and domain browse (gap closure).
- [x] 05-09-PLAN.md - Refactor crawler wiki snapshot to dynamic section/headings schema and simplified current counters (gap closure).
- [ ] 05-10-PLAN.md - Refactor API wiki persistence/response to dynamic sections, dynamic anchors, and simplified counters (gap closure).
- [ ] 05-11-PLAN.md - Refactor frontend wiki rendering to dynamic headings/anchors and remove star-history timeline from wiki surface (gap closure).
- [ ] 05-12-PLAN.md - Establish API-owned wiki authoring persistence/workflow and published-read source (gap closure).
- [ ] 05-13-PLAN.md - Add admin-only draft/edit/publish/rollback API lifecycle with version history and no review/citation gate (gap closure).
- [ ] 05-14-PLAN.md - Re-scope crawler to freshness signals only and wire API-side regeneration triggers via webhook (gap closure).
- [ ] 05-15-PLAN.md - Align frontend admin lifecycle UI with citation-free publish behavior (gap closure).
- [ ] 05-16-PLAN.md - Enforce API role matrix for draft visibility and admin-only lifecycle actions (gap closure).
- [ ] 05-17-PLAN.md - Align public wiki route to published-only reading while preserving baseline UX behavior (gap closure).

### Phase 6: I want to improve our wikichat and retreval service (controlloer) for better quillity for users while chatting

**Goal:** Improve the existing wiki chat flow so answers stay Korean-only, repository-grounded, clarification-aware, and resilient across follow-up turns without expanding the compact `/api/wiki/projects/*/chat` surface.
**Requirements**: CHAT-QUAL-01, CHAT-QUAL-02, CHAT-QUAL-03, CHAT-QUAL-04, CHAT-QUAL-05, CHAT-QUAL-06
**Depends on:** Phase 5
**Success Criteria** (what must be TRUE):
  1. Broad or ambiguous repo questions return a safe grounded answer first, then at most one narrow clarification with 2-3 short options when needed.
  2. Follow-up answers rely mainly on the last 2-3 relevant turns, reset cleanly on project mismatch or topic shift, and still support explicit fresh starts within the short session window.
  3. Partial retrieval still produces short grounded guidance with better-next-question suggestions instead of empty-context or generic-chat failure modes.
  4. The authenticated wiki chat contract remains compact (`answer`, `isClarification`, `sessionId`) while real replies stay Korean-only, concise, and repository-specific.
**Plans:** 4 plans

Plans:
- [x] 06-01-PLAN.md - Define internal wiki chat quality contracts and wave-1 regression scaffolds.
- [x] 06-02-PLAN.md - Harden project-scoped session memory and recent-turn selection behavior.
- [x] 06-03-PLAN.md - Upgrade retrieval assembly, reranking, and weak-grounding fallback behavior.
- [x] 06-04-PLAN.md - Rework chat orchestration and controller mapping to deliver Korean grounded answers with deferred final live verification.
