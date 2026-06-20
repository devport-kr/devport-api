package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * IP-based anonymous rate limiter for wiki chat: 1 request/day per IP. Fail-open on Redis errors.
 */
@Component
class WikiAnonRateLimiter(
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun checkAndIncrement(ip: String) {
        val key = "$KEY_PREFIX$ip:daily"
        try {
            val count = redisTemplate.opsForValue().increment(key)
            if (count == null) {
                log.warn("wiki-anon-rl: Redis returned null for key={}, allowing request", key)
                return
            }
            if (count == 1L) {
                redisTemplate.expire(key, DAILY_WINDOW)
            }
            if (count > DAILY_LIMIT) {
                throw WikiChatRateLimitExceededException(
                    "익명 사용자는 하루 1번만 질문할 수 있습니다. 로그인하면 더 많이 이용할 수 있어요.",
                )
            }
        } catch (e: WikiChatRateLimitExceededException) {
            throw e
        } catch (e: Exception) {
            log.error("wiki-anon-rl: Redis error for key={}, allowing request: {}", key, e.message)
        }
    }

    companion object {
        private const val DAILY_LIMIT = 1L
        private val DAILY_WINDOW: Duration = Duration.ofHours(24)
        private const val KEY_PREFIX = "wiki:anon:ip:"
    }
}
