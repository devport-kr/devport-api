package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiChatRateLimiter {

    static final int LIMIT = 5;
    static final int DAILY_LIMIT = 100;
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final Duration DAILY_WINDOW = Duration.ofHours(24);
    private static final String KEY_PREFIX = "wiki:rl:";
    private static final String DAILY_KEY_PREFIX = "wiki:rl:day:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Dual fixed-window rate limit per user:
     * - 5 requests per 60 seconds (burst protection)
     * - 100 requests per 24 hours (sustained abuse protection)
     * Fail-open on Redis errors to avoid blocking chat when Redis is unavailable.
     *
     * @param userId authenticated user ID string
     * @throws WikiChatRateLimitExceededException when either limit is exceeded
     */
    public void check(String userId) {
        checkWindow(KEY_PREFIX + userId, LIMIT, WINDOW,
                "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        checkWindow(DAILY_KEY_PREFIX + userId, DAILY_LIMIT, DAILY_WINDOW,
                "일일 요청 한도를 초과했습니다. 내일 다시 시도해 주세요.");
    }

    private void checkWindow(String key, int limit, Duration window, String errorMessage) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                log.warn("wiki-rl: Redis returned null for key={}, allowing request", key);
                return;
            }
            if (count == 1L) {
                redisTemplate.expire(key, window);
            }
            if (count > limit) {
                throw new WikiChatRateLimitExceededException(errorMessage);
            }
        } catch (WikiChatRateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("wiki-rl: Redis error for key={}, allowing request: {}", key, e.getMessage());
        }
    }
}
