package kr.devport.api.domain.wiki.infrastructure

import java.time.Duration

/**
 * Out-port: fixed-window hit counter for rate limiting. Implemented by :wiki:adapter-redis.
 * Rate-limit *policy* (limits, windows, messages) lives in the wiki core; only the increment/TTL
 * mechanics live in the adapter.
 */
interface RateLimitCounter {
    /**
     * Increment the counter for [key]; on the first hit in the window, set its TTL to [window].
     * Returns the new count, or null when the backing store is unavailable (callers fail open).
     */
    fun hit(
        key: String,
        window: Duration,
    ): Long?
}
