package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.infrastructure.RateLimitCounter
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * IP-based anonymous rate limiter for wiki chat: 1 request/day per IP. Holds only the policy; the
 * Redis increment/TTL mechanics live behind [RateLimitCounter]. Fails open when the counter is null.
 */
@Component
class WikiAnonRateLimiter(
    private val rateLimitCounter: RateLimitCounter,
) {
    fun checkAndIncrement(ip: String) {
        val key = "$KEY_PREFIX$ip:daily"
        val count = rateLimitCounter.hit(key, DAILY_WINDOW) ?: return
        if (count > DAILY_LIMIT) {
            throw WikiChatRateLimitExceededException(
                "익명 사용자는 하루 1번만 질문할 수 있습니다. 로그인하면 더 많이 이용할 수 있어요.",
            )
        }
    }

    companion object {
        private const val DAILY_LIMIT = 1L
        private val DAILY_WINDOW: Duration = Duration.ofHours(24)
        private const val KEY_PREFIX = "wiki:anon:ip:"
    }
}
