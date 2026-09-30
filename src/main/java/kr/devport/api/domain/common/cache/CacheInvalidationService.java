package kr.devport.api.domain.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Coordinates scope-based cache invalidation with retry/backoff and uncertainty tracking.
 * 
 * Invalidates related cache groups (detail + list + summary) when crawler signals data changes.
 * Tracks invalidation failures as uncertainty states for downstream read-through control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final long MAX_BACKOFF_MS = 2000;
    
    private final CacheManager cacheManager;
    private final CacheFallbackStateStore fallbackStateStore;
    
    /**
     * Invalidates all caches for the given scope with retry/backoff.
     * 
     * For known scopes (ARTICLE, GIT_REPO, LLM): invalidates related cache groups.
     * For UNKNOWN scope: broad invalidation of all critical caches for safety.
     * 
     * Marks scope as uncertain during retry windows.
     * Clears uncertainty state after successful invalidation.
     * 
     * @param scope Cache scope to invalidate
     * @param jobId Job identifier for logging and idempotency
     */
    public void invalidateScope(CacheScope scope, String jobId) {
        Set<String> cacheNames = CacheGroups.forScope(scope);
        
        if (cacheNames.isEmpty()) {
            log.warn("No cache groups mapped for scope={}, jobId={}", scope, jobId);
            return;
        }
        
        log.info("Starting invalidation for scope={}, caches={}, jobId={}", 
            scope, cacheNames, jobId);
        
        boolean success = false;
        int attempt = 0;
        
        while (attempt < MAX_RETRY_ATTEMPTS && !success) {
            attempt++;
            
            try {
                // Mark as uncertain before invalidation attempt
                if (attempt > 1) {
                    fallbackStateStore.markUncertain(scope, jobId);
                }
                
                // Invalidate all caches in the scope
                for (String cacheName : cacheNames) {
                    Cache cache = cacheManager.getCache(cacheName);
                    
                    if (cache != null) {
                        cache.clear();
                        log.debug("Cleared cache: {} (scope={}, attempt={}, jobId={})", 
                            cacheName, scope, attempt, jobId);
                    } else {
                        log.warn("Cache not found: {} (scope={}, jobId={})", 
                            cacheName, scope, jobId);
                    }
                }
                
                success = true;
                
                // Clear uncertainty state on success
                fallbackStateStore.clearUncertainty(scope, jobId);
                
                log.info("Successfully invalidated scope={} after {} attempt(s), jobId={}", 
                    scope, attempt, jobId);
                
            } catch (Exception e) {
                log.error("Invalidation attempt {} failed for scope={}, jobId={}", 
                    attempt, scope, jobId, e);
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    long backoffMs = calculateBackoff(attempt);
                    log.info("Retrying invalidation after {}ms (attempt {}/{})", 
                        backoffMs, attempt + 1, MAX_RETRY_ATTEMPTS);
                    
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Backoff interrupted, aborting retries");
                        break;
                    }
                } else {
                    log.error("Max retry attempts reached for scope={}, jobId={}. " +
                        "Scope marked uncertain until TTL expiry.", scope, jobId);
                    
                    // Keep uncertainty state - will be cleared on next successful invalidation
                    fallbackStateStore.markUncertain(scope, jobId);
                }
            }
        }
    }
    
    /**
     * Calculates exponential backoff with jitter.
     */
    private long calculateBackoff(int attempt) {
        long exponentialBackoff = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
        long cappedBackoff = Math.min(exponentialBackoff, MAX_BACKOFF_MS);
        
        // Add jitter (±25%)
        double jitterFactor = 0.75 + (Math.random() * 0.5);
        return (long) (cappedBackoff * jitterFactor);
    }
    
    /**
     * Checks if a scope is currently in an uncertain state.
     * Downstream read paths can use this to temporarily bypass cache.
     * 
     * @param scope Cache scope to check
     * @return true if invalidation is uncertain/failed for this scope
     */
    public boolean isUncertain(CacheScope scope) {
        return fallbackStateStore.isUncertain(scope);
    }
}
