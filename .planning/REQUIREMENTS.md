# Requirements: DevPort API Stabilization Milestone

**Defined:** 2026-02-14
**Core Value:** API behavior stays predictable and debuggable under normal and failure conditions.

## v1 Requirements

### Exception Contracts

- [ ] **EXC-01**: Domain/service paths in scoped modules do not throw broad `RuntimeException`/`IllegalArgumentException` for expected business failures.
- [ ] **EXC-02**: Every scoped domain exception maps to a deterministic HTTP status and structured error payload.
- [ ] **EXC-03**: Error payload includes stable fields (`type`, `title`, `status`, `detail`, `instance`) for all mapped domain failures.
- [ ] **EXC-04**: Unknown/unhandled failures still return sanitized server errors without sensitive internals.

### Cache Correctness

- [ ] **CACH-01**: Cache names used by read/write paths are centralized and consistent across annotations and services.
- [ ] **CACH-02**: Cache invalidation is triggered for all write/update/delete operations that affect cached read endpoints.
- [ ] **CACH-03**: Cache key composition is deterministic and includes relevant request dimensions to prevent cross-query collisions.
- [ ] **CACH-04**: Cache TTL policy is explicitly defined for each critical cache and documented in code/config.
- [ ] **CACH-05**: Cache clearing strategy avoids unsafe production patterns and is verified by integration tests.

### Regression Safety

- [ ] **TEST-01**: Integration tests verify exception-to-status mapping for representative auth/article/project error cases.
- [ ] **TEST-02**: Integration tests verify cache invalidation after mutation operations for affected endpoints.
- [ ] **TEST-03**: Regression tests cover at least one rollback/failure path where stale cache would previously occur.

### Observability and Security Hardening

- [ ] **OBSV-01**: Logs include request correlation identifiers and exception class metadata for scoped failure paths.
- [ ] **OBSV-02**: Metrics exist for exception-class counts and cache eviction/miss behavior on critical caches.
- [ ] **SECU-01**: Auth-sensitive endpoints in scope have explicit hardening checks (validation, abuse-resistance guardrails) with tests.

### Wiki Chat Quality

- [x] **CHAT-QUAL-01**: Broad but partly answerable wiki-chat questions return the safest grounded slice first and then, when still needed, one narrow Korean follow-up instead of immediate refusal or open-ended looping.
- [x] **CHAT-QUAL-02**: Ambiguous repo-area questions return 2-3 short Korean narrowing options, and clarification state is produced by the service contract rather than inferred from punctuation.
- [x] **CHAT-QUAL-03**: Wiki chat memory is project-scoped and short-lived: the last 2-3 relevant turns dominate, project mismatches reset memory, and stale context can be dropped on topic shift.
- [x] **CHAT-QUAL-04**: Retrieval assembles diverse grounded repo context from `wiki_section_chunks` and degrades gracefully so partial retrieval still yields short grounded guidance plus 2-3 better next questions.
- [x] **CHAT-QUAL-05**: The existing compact authenticated wiki chat surface (`/api/wiki/projects/*/chat` plus session clear) remains stable while supporting resume and intentional fresh starts within the current short session window.
- [x] **CHAT-QUAL-06**: Final wiki chat replies remain Korean-only, concise, teammate-like, and mention concrete repo files/classes/methods when grounding supports it.

## v2 Requirements

### Operational Maturity

- **OPER-01**: CI release gate enforces error-contract regression checks for critical endpoints.
- **OPER-02**: Stability scorecard dashboard tracks 5xx rate, cache hit ratio, and auth-failure trends over time.
- **OPER-03**: Incident runbook documents triage/remediation steps for top exception and cache failure classes.

## Out of Scope

| Feature | Reason |
|---------|--------|
| New user-facing API features | This milestone is stabilization-only. |
| Major architecture rewrite (microservice split/event-platform migration) | High risk for solo project; does not directly solve immediate reliability debt. |
| Auth provider/platform replacement | Replatforming auth is too large for this milestone; harden current implementation first. |
| Broad performance-optimization initiative | Correctness and predictability take priority over speed optimization in this milestone. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| EXC-01 | Phase 1 | Pending |
| EXC-02 | Phase 1 | Pending |
| EXC-03 | Phase 1 | Pending |
| EXC-04 | Phase 1 | Pending |
| CACH-01 | Phase 2 | Pending |
| CACH-02 | Phase 2 | Pending |
| CACH-03 | Phase 2 | Pending |
| CACH-04 | Phase 2 | Pending |
| CACH-05 | Phase 2 | Pending |
| TEST-01 | Phase 3 | Pending |
| TEST-02 | Phase 3 | Pending |
| TEST-03 | Phase 3 | Pending |
| OBSV-01 | Phase 4 | Pending |
| OBSV-02 | Phase 4 | Pending |
| SECU-01 | Phase 4 | Pending |
| CHAT-QUAL-01 | Phase 6 | Complete |
| CHAT-QUAL-02 | Phase 6 | Complete |
| CHAT-QUAL-03 | Phase 6 | Complete |
| CHAT-QUAL-04 | Phase 6 | Complete |
| CHAT-QUAL-05 | Phase 6 | Complete |
| CHAT-QUAL-06 | Phase 6 | Complete |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0 ✓

---
*Requirements defined: 2026-02-14*
*Last updated: 2026-03-08 after Phase 6 roadmap mapping*
