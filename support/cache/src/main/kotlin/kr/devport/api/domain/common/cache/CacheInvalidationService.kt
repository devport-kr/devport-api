package kr.devport.api.domain.common.cache

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import kotlin.math.min
import kotlin.math.pow

/**
 * Coordinates scope-based cache invalidation with retry/backoff and uncertainty tracking. Invalidates
 * related cache groups when the crawler signals a data change; tracks failures as uncertainty state.
 */
@Service
class CacheInvalidationService(
    private val cacheManager: CacheManager,
    private val fallbackStateStore: CacheFallbackStateStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Invalidate all caches for the scope with retry/backoff; marks the scope uncertain during retries. */
    fun invalidateScope(
        scope: CacheScope,
        jobId: String,
    ) {
        val cacheNames = CacheGroups.forScope(scope)
        if (cacheNames.isEmpty()) {
            log.warn("No cache groups mapped for scope={}, jobId={}", scope, jobId)
            return
        }

        log.info("Starting invalidation for scope={}, caches={}, jobId={}", scope, cacheNames, jobId)

        var success = false
        var attempt = 0

        while (attempt < MAX_RETRY_ATTEMPTS && !success) {
            attempt++
            try {
                if (attempt > 1) {
                    fallbackStateStore.markUncertain(scope, jobId)
                }

                for (cacheName in cacheNames) {
                    val cache = cacheManager.getCache(cacheName)
                    if (cache != null) {
                        cache.clear()
                        log.debug("Cleared cache: {} (scope={}, attempt={}, jobId={})", cacheName, scope, attempt, jobId)
                    } else {
                        log.warn("Cache not found: {} (scope={}, jobId={})", cacheName, scope, jobId)
                    }
                }

                success = true
                fallbackStateStore.clearUncertainty(scope, jobId)
                log.info("Successfully invalidated scope={} after {} attempt(s), jobId={}", scope, attempt, jobId)
            } catch (e: Exception) {
                log.error("Invalidation attempt {} failed for scope={}, jobId={}", attempt, scope, jobId, e)

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    val backoffMs = calculateBackoff(attempt)
                    log.info("Retrying invalidation after {}ms (attempt {}/{})", backoffMs, attempt + 1, MAX_RETRY_ATTEMPTS)
                    try {
                        Thread.sleep(backoffMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        log.warn("Backoff interrupted, aborting retries")
                        break
                    }
                } else {
                    log.error(
                        "Max retry attempts reached for scope={}, jobId={}. Scope marked uncertain until TTL expiry.",
                        scope,
                        jobId,
                    )
                    fallbackStateStore.markUncertain(scope, jobId)
                }
            }
        }
    }

    /** Exponential backoff with ±25% jitter. */
    private fun calculateBackoff(attempt: Int): Long {
        val exponentialBackoff = INITIAL_BACKOFF_MS * 2.0.pow(attempt - 1).toLong()
        val cappedBackoff = min(exponentialBackoff, MAX_BACKOFF_MS)
        val jitterFactor = 0.75 + (Math.random() * 0.5)
        return (cappedBackoff * jitterFactor).toLong()
    }

    fun isUncertain(scope: CacheScope): Boolean = fallbackStateStore.isUncertain(scope)

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_BACKOFF_MS = 100L
        private const val MAX_BACKOFF_MS = 2000L
    }
}
