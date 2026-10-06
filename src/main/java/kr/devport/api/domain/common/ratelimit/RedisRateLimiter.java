package kr.devport.api.domain.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis INCR 기반 고정 윈도우 rate limiter.
 * Redis 장애 시 동작은 호출자가 고른다: {@link #tryAcquire}는 허용(fail-open),
 * {@link #acquire}는 {@link Result#UNAVAILABLE}을 돌려줘 거부(fail-closed)할 수 있게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final long NO_EXPIRE = -1L;

    public enum Result {
        ALLOWED,
        DENIED,
        /** Redis 오류로 판단 불가 */
        UNAVAILABLE
    }

    private final RedisTemplate<String, Object> redisTemplate;

    public Result acquire(String key, int limit, Duration window) {
        String redisKey = KEY_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return Result.UNAVAILABLE;
            }
            if (count == 1L) {
                redisTemplate.expire(redisKey, window);
            } else if (count > limit && Long.valueOf(NO_EXPIRE).equals(redisTemplate.getExpire(redisKey))) {
                // INCR 후 EXPIRE 전에 실패해 TTL 없이 남은 키가 영구 차단되지 않도록 복구한다.
                redisTemplate.expire(redisKey, window);
                log.warn("rate-limit: restored missing TTL for key={}", redisKey);
            }
            return count <= limit ? Result.ALLOWED : Result.DENIED;
        } catch (Exception e) {
            log.warn("rate-limit: Redis error for key={}: {}", redisKey, e.getMessage());
            return Result.UNAVAILABLE;
        }
    }

    /**
     * Redis 장애 시 요청을 막지 않는다(fail-open).
     *
     * @return 윈도우 내 호출 횟수가 limit 이하이거나 Redis를 쓸 수 없으면 true
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        return acquire(key, limit, window) != Result.DENIED;
    }
}
