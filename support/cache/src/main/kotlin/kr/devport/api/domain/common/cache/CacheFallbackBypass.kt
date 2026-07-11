package kr.devport.api.domain.common.cache

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * SpEL-friendly bypass policy for scoped uncertainty state. Used in @Cacheable `unless`/`condition`
 * to fail safe: uncertain scope → bypass (read-through), stable scope → cache allowed.
 *
 * e.g. `unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')"`.
 */
@Component("cacheFallbackBypass")
class CacheFallbackBypass(
    private val stateStore: CacheFallbackStateStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Bypass for a scope name (e.g. "ARTICLE"); unknown names fail safe to UNKNOWN. */
    fun shouldBypass(scopeName: String): Boolean =
        try {
            shouldBypass(CacheScope.valueOf(scopeName))
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid scope name: {}, treating as UNKNOWN", scopeName)
            shouldBypass(CacheScope.UNKNOWN)
        }

    fun shouldBypass(scope: CacheScope): Boolean {
        val uncertain = stateStore.isUncertain(scope)
        if (uncertain) {
            log.debug("Bypassing cache for scope={} due to uncertainty", scope)
        }
        return uncertain
    }

    fun allowCache(scopeName: String): Boolean = !shouldBypass(scopeName)

    fun allowCache(scope: CacheScope): Boolean = !shouldBypass(scope)
}
