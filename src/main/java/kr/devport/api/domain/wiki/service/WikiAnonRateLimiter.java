package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * IP-based anonymous rate limiter for wiki chat.
 * Allows 1 request per day per IP address.
 * Fail-open on Redis errors to avoid blocking chat when Redis is unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiAnonRateLimiter {

    private static final int DAILY_LIMIT = 1;
    private static final Duration DAILY_WINDOW = Duration.ofHours(24);
    private static final String KEY_PREFIX = "wiki:anon:ip:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check and increment the daily request count for the given IP.
     *
     * @param ip Client IP address
     * @throws WikiChatRateLimitExceededException when daily limit is exceeded
     */
    public void checkAndIncrement(String ip) {
        String key = KEY_PREFIX + ip + ":daily";
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                log.warn("wiki-anon-rl: Redis returned null for key={}, allowing request", key);
                return;
            }
            if (count == 1L) {
                redisTemplate.expire(key, DAILY_WINDOW);
            }
            if (count > DAILY_LIMIT) {
                throw new WikiChatRateLimitExceededException(
                        "익명 사용자는 하루 1번만 질문할 수 있습니다. 로그인하면 더 많이 이용할 수 있어요.");
            }
        } catch (WikiChatRateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("wiki-anon-rl: Redis error for key={}, allowing request: {}", key, e.getMessage());
        }
    }
}
