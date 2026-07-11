package kr.devport.api.domain.wiki.adapter.redis

import kr.devport.api.domain.wiki.infrastructure.RateLimitCounter
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis adapter for [RateLimitCounter]: increments a fixed-window counter and sets its TTL on the
 * first hit. Returns null on any Redis failure so the wiki rate limiters fail open.
 */
@Component
class RedisRateLimitCounter(
    private val redisTemplate: RedisTemplate<String, Any>,
) : RateLimitCounter {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun hit(
        key: String,
        window: Duration,
    ): Long? =
        try {
            val count = redisTemplate.opsForValue().increment(key)
            if (count == null) {
                log.warn("wiki-rl: Redis returned null for key={}, allowing request", key)
            } else if (count == 1L) {
                redisTemplate.expire(key, window)
            }
            count
        } catch (e: Exception) {
            log.error("wiki-rl: Redis error for key={}, allowing request: {}", key, e.message)
            null
        }
}
