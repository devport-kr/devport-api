package kr.devport.api.domain.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks temporary uncertainty state for cache scopes during invalidation failures.
 * 
 * When invalidation fails or is retrying, affected scopes are marked uncertain.
 * Downstream read paths can query this state to decide whether to bypass cache
 * and read from source of truth.
 * 
 * Uncertainty is cleared when invalidation succeeds or TTL expires naturally.
 */
@Component
@Slf4j
public class CacheFallbackStateStore {
    
    /**
     * Maximum age of uncertainty state before auto-clearing (5 minutes).
     * After this duration, assume TTL has handled staleness.
     */
    private static final long MAX_UNCERTAINTY_AGE_MS = 5 * 60 * 1000;
    
    /**
     * Map of scope -> uncertainty metadata.
     */
    private final Map<CacheScope, UncertaintyState> uncertaintyMap = new ConcurrentHashMap<>();
    
    /**
     * Marks a scope as uncertain due to invalidation failure/retry.
     * 
     * @param scope Cache scope experiencing invalidation issues
     * @param jobId Job identifier causing the uncertainty
     */
    public void markUncertain(CacheScope scope, String jobId) {
        UncertaintyState state = new UncertaintyState(jobId, Instant.now());
        uncertaintyMap.put(scope, state);
        
        log.warn("Marked scope={} as uncertain due to jobId={}", scope, jobId);
    }
    
    /**
     * Clears uncertainty for a scope after successful invalidation.
     * 
     * @param scope Cache scope that was successfully invalidated
     * @param jobId Job identifier that triggered the invalidation
     */
    public void clearUncertainty(CacheScope scope, String jobId) {
        UncertaintyState removed = uncertaintyMap.remove(scope);
        
        if (removed != null) {
            log.info("Cleared uncertainty for scope={}, jobId={} (was uncertain since {})", 
                scope, jobId, removed.markedAt);
        }
    }
    
    /**
     * Checks if a scope is currently in uncertain state.
     * 
     * Auto-clears stale uncertainty states older than MAX_UNCERTAINTY_AGE_MS.
     * 
     * @param scope Cache scope to check
     * @return true if scope is uncertain and uncertainty is recent
     */
    public boolean isUncertain(CacheScope scope) {
        UncertaintyState state = uncertaintyMap.get(scope);
        
        if (state == null) {
            return false;
        }
        
        // Auto-clear stale uncertainty (assume TTL has handled it)
        long ageMs = Instant.now().toEpochMilli() - state.markedAt.toEpochMilli();
        if (ageMs > MAX_UNCERTAINTY_AGE_MS) {
            log.debug("Auto-clearing stale uncertainty for scope={} (age: {}ms)", scope, ageMs);
            uncertaintyMap.remove(scope);
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns the job ID that caused the current uncertainty, if any.
     * 
     * @param scope Cache scope to query
     * @return Job ID or null if not uncertain
     */
    public String getUncertainJobId(CacheScope scope) {
        UncertaintyState state = uncertaintyMap.get(scope);
        return state != null ? state.jobId : null;
    }
    
    /**
     * Metadata about uncertainty state.
     */
    private record UncertaintyState(String jobId, Instant markedAt) {}
}
