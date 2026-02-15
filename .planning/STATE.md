# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-02-14)

**Core value:** API behavior stays predictable and debuggable under normal and failure conditions.
**Current focus:** Phase 5 - Code Wiki (new-phase1)

## Current Position

Phase: 5 (new-phase1 - IN PROGRESS)
Plan: 3 of 6 in current phase
Status: Plan 05-01 complete (wiki snapshot foundation), Plan 05-03 complete (API read endpoints), Plan 05-04 complete (wiki chat API)
Last activity: 2026-02-15 - Completed plan 05-04: wiki chat API with grounded retrieval, session memory, and uncertainty handling.

Progress: [██████████] 100% (milestone phases 1-4) + Phase 5: [█████░] 50%

## Performance Metrics

**Velocity:**
- Total plans completed: 9 (6 from phases 1-4, 3 from phase 5)
- Average duration: 4.6 min
- Total execution time: 0.68 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 0 | - | - |
| 2 | 6 | 27 min | 4.5 min |
| 3 | 0 | - | - |
| 4 | 0 | - | - |
| 5 | 3 | 14 min | 4.7 min |

**Recent Trend:**
- Last 8 plans: 02-02 (4 min), 02-05 (2 min), 02-03 (3 min), 02-06 (3 min), 02-04 (11 min), 05-01 (4 min), 05-03 (4 min), 05-04 (6 min)
- Trend: Phase 5 (Code Wiki) in progress - 3 of 6 plans complete

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

### Roadmap Evolution

- Phase 5 added: new phase1

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-15
Stopped at: Completed 05-04-PLAN.md (wiki chat API)
Resume file: None
Next: Execute 05-02-PLAN.md (crawler wiki generation), 05-05-PLAN.md (frontend), 05-06-PLAN.md (quality gates)
