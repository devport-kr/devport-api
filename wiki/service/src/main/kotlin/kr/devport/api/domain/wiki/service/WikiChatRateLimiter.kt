package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.infrastructure.RateLimitCounter
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Dual fixed-window per-user rate limit: 5 req/60s (burst) and 100 req/24h (sustained). Holds only
 * the policy; the Redis increment/TTL mechanics live behind [RateLimitCounter]. Fails open when the
 * counter is null.
 */
@Component
class WikiChatRateLimiter(
    private val rateLimitCounter: RateLimitCounter,
) {
    fun check(userId: String) {
        checkWindow(KEY_PREFIX + userId, LIMIT, WINDOW, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
        checkWindow(DAILY_KEY_PREFIX + userId, DAILY_LIMIT, DAILY_WINDOW, "일일 요청 한도를 초과했습니다. 내일 다시 시도해 주세요.")
    }

    private fun checkWindow(
        key: String,
        limit: Int,
        window: Duration,
        errorMessage: String,
    ) {
        val count = rateLimitCounter.hit(key, window) ?: return
        if (count > limit) {
            throw WikiChatRateLimitExceededException(errorMessage)
        }
    }

    companion object {
        const val LIMIT = 5
        const val DAILY_LIMIT = 100
        private val WINDOW: Duration = Duration.ofSeconds(60)
        private val DAILY_WINDOW: Duration = Duration.ofHours(24)
        private const val KEY_PREFIX = "wiki:rl:"
        private const val DAILY_KEY_PREFIX = "wiki:rl:day:"
    }
}
