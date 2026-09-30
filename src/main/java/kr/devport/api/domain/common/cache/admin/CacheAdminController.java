package kr.devport.api.domain.common.cache.admin;

import kr.devport.api.domain.common.cache.CacheFallbackStateStore;
import kr.devport.api.domain.common.cache.CacheScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin/internal-only cache fallback controls.
 * 
 * Provides endpoints for inspecting and overriding fallback state.
 * All endpoints are restricted to admin/internal routes per SecurityConfig.
 * 
 * DO NOT expose equivalent public endpoints.
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheAdminController {
    
    private final CacheFallbackStateStore stateStore;
    
    /**
     * GET /api/admin/cache/fallback/status
     * 
     * Returns current uncertainty state for all scopes.
     * 
     * Response:
     * {
     *   "ARTICLE": { "uncertain": true, "jobId": "job-123" },
     *   "GIT_REPO": { "uncertain": false, "jobId": null },
     *   "LLM": { "uncertain": false, "jobId": null },
     *   "UNKNOWN": { "uncertain": false, "jobId": null }
     * }
     */
    @GetMapping("/fallback/status")
    public ResponseEntity<Map<String, ScopeStatus>> getFallbackStatus() {
        Map<String, ScopeStatus> status = new HashMap<>();
        int uncertainScopeCount = 0;
        
        for (CacheScope scope : CacheScope.values()) {
            boolean uncertain = stateStore.isUncertain(scope);
            String jobId = stateStore.getUncertainJobId(scope);
            if (uncertain) {
                uncertainScopeCount++;
            }
            status.put(scope.name(), new ScopeStatus(uncertain, jobId));
        }
        
        log.debug("Admin queried fallback status for {} scopes (uncertainScopes={})",
            status.size(), uncertainScopeCount);
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/admin/cache/fallback/status/{scope}
     * 
     * Returns uncertainty state for a specific scope.
     * 
     * Path parameter:
     *   scope - ARTICLE, GIT_REPO, LLM, or UNKNOWN
     * 
     * Response:
     * {
     *   "scope": "ARTICLE",
     *   "uncertain": true,
     *   "jobId": "job-123"
     * }
     */
    @GetMapping("/fallback/status/{scope}")
    public ResponseEntity<ScopeStatusDetail> getFallbackStatusForScope(@PathVariable String scope) {
        try {
            CacheScope cacheScope = CacheScope.valueOf(scope.toUpperCase());
            boolean uncertain = stateStore.isUncertain(cacheScope);
            String jobId = stateStore.getUncertainJobId(cacheScope);
            
            ScopeStatusDetail detail = new ScopeStatusDetail(
                cacheScope.name(),
                uncertain,
                jobId
            );
            
            log.debug("Admin queried fallback status for scope={}: uncertain={}, jobId={}",
                scope, uncertain, jobId);
            
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            log.warn("Admin requested invalid scope: {}", scope);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * POST /api/admin/cache/fallback/override/{scope}
     * 
     * Manually marks a scope as uncertain (for testing/emergency).
     * 
     * Path parameter:
     *   scope - ARTICLE, GIT_REPO, LLM, or UNKNOWN
     * 
     * Request body:
     * {
     *   "jobId": "manual-override-reason"
     * }
     * 
     * Response:
     * {
     *   "scope": "ARTICLE",
     *   "action": "marked_uncertain",
     *   "jobId": "manual-override-reason"
     * }
     */
    @PostMapping("/fallback/override/{scope}")
    public ResponseEntity<OverrideResult> markScopeUncertain(
        @PathVariable String scope,
        @RequestBody OverrideRequest request
    ) {
        try {
            CacheScope cacheScope = CacheScope.valueOf(scope.toUpperCase());
            String jobId = request.jobId() != null ? request.jobId() : "manual-override";
            
            stateStore.markUncertain(cacheScope, jobId);
            
            OverrideResult result = new OverrideResult(
                cacheScope.name(),
                "marked_uncertain",
                jobId
            );
            
            log.info("Admin manually marked scope={} as uncertain with jobId={}", scope, jobId);
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Admin requested invalid scope for override: {}", scope);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * DELETE /api/admin/cache/fallback/override/{scope}
     * 
     * Clears uncertainty for a scope (manual recovery).
     * 
     * Path parameter:
     *   scope - ARTICLE, GIT_REPO, LLM, or UNKNOWN
     * 
     * Response:
     * {
     *   "scope": "ARTICLE",
     *   "action": "cleared_uncertainty",
     *   "jobId": "manual-clear"
     * }
     */
    @DeleteMapping("/fallback/override/{scope}")
    public ResponseEntity<OverrideResult> clearScopeUncertainty(@PathVariable String scope) {
        try {
            CacheScope cacheScope = CacheScope.valueOf(scope.toUpperCase());
            String jobId = "manual-clear";
            
            stateStore.clearUncertainty(cacheScope, jobId);
            
            OverrideResult result = new OverrideResult(
                cacheScope.name(),
                "cleared_uncertainty",
                jobId
            );
            
            log.info("Admin manually cleared uncertainty for scope={}", scope);
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Admin requested invalid scope for clear: {}", scope);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // ========== DTOs ==========
    
    /**
     * Status of a single scope (for list view).
     */
    record ScopeStatus(
        boolean uncertain,
        String jobId
    ) {}
    
    /**
     * Detailed status of a specific scope.
     */
    record ScopeStatusDetail(
        String scope,
        boolean uncertain,
        String jobId
    ) {}
    
    /**
     * Request body for manual override.
     */
    record OverrideRequest(
        String jobId
    ) {}
    
    /**
     * Result of override/clear operation.
     */
    record OverrideResult(
        String scope,
        String action,
        String jobId
    ) {}
}
