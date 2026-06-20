package kr.devport.api.domain.common.cache

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks temporary uncertainty state for cache scopes during invalidation failures. Read paths can
 * query this to decide whether to bypass the cache. Uncertainty clears on success or after a TTL.
 */
@Component
class CacheFallbackStateStore {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uncertaintyMap = ConcurrentHashMap<CacheScope, UncertaintyState>()

    /** Mark a scope uncertain due to an invalidation failure/retry. */
    fun markUncertain(
        scope: CacheScope,
        jobId: String,
    ) {
        uncertaintyMap[scope] = UncertaintyState(jobId, Instant.now())
        log.warn("Marked scope={} as uncertain due to jobId={}", scope, jobId)
    }

    /** Clear uncertainty for a scope after a successful invalidation. */
    fun clearUncertainty(
        scope: CacheScope,
        jobId: String,
    ) {
        val removed = uncertaintyMap.remove(scope)
        if (removed != null) {
            log.info("Cleared uncertainty for scope={}, jobId={} (was uncertain since {})", scope, jobId, removed.markedAt)
        }
    }

    /** True if the scope is uncertain and the uncertainty is still recent (auto-clears stale state). */
    fun isUncertain(scope: CacheScope): Boolean {
        val state = uncertaintyMap[scope] ?: return false

        val ageMs = Instant.now().toEpochMilli() - state.markedAt.toEpochMilli()
        if (ageMs > MAX_UNCERTAINTY_AGE_MS) {
            log.debug("Auto-clearing stale uncertainty for scope={} (age: {}ms)", scope, ageMs)
            uncertaintyMap.remove(scope)
            return false
        }

        return true
    }

    /** The job ID that caused the current uncertainty, or null. */
    fun getUncertainJobId(scope: CacheScope): String? = uncertaintyMap[scope]?.jobId

    private data class UncertaintyState(
        val jobId: String,
        val markedAt: Instant,
    )

    companion object {
        /** Max age of uncertainty state before auto-clearing (5 minutes). */
        private const val MAX_UNCERTAINTY_AGE_MS = 5L * 60 * 1000
    }
}
