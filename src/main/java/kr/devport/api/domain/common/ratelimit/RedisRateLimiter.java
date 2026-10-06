package kr.devport.api.domain.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis INCR 기반 고정 윈도우 rate limiter.
 * Redis 장애 시에는 요청을 막지 않는다(fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * @return 윈도우 내 호출 횟수가 limit 이하이면 true
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        String redisKey = KEY_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(redisKey, window);
            }
            return count <= limit;
        } catch (Exception e) {
            log.warn("rate-limit: Redis error for key={}, allowing request: {}", redisKey, e.getMessage());
            return true;
        }
    }
}
