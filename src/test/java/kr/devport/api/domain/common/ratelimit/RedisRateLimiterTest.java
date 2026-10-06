package kr.devport.api.domain.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisRateLimiterTest {

    private static final String KEY = "ratelimit:test";
    private static final Duration WINDOW = Duration.ofMinutes(5);

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void firstHitStartsWindow() {
        when(valueOperations.increment(KEY)).thenReturn(1L);

        assertThat(rateLimiter.acquire("test", 2, WINDOW)).isEqualTo(RedisRateLimiter.Result.ALLOWED);
        verify(redisTemplate).expire(KEY, WINDOW);
    }

    @Test
    void deniesAboveLimit() {
        when(valueOperations.increment(KEY)).thenReturn(3L);
        when(redisTemplate.getExpire(KEY)).thenReturn(120L);

        assertThat(rateLimiter.acquire("test", 2, WINDOW)).isEqualTo(RedisRateLimiter.Result.DENIED);
        assertThat(rateLimiter.tryAcquire("test", 2, WINDOW)).isFalse();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void restoresMissingTtlSoKeyCannotBlockForever() {
        when(valueOperations.increment(KEY)).thenReturn(50L);
        when(redisTemplate.getExpire(KEY)).thenReturn(-1L);

        assertThat(rateLimiter.acquire("test", 2, WINDOW)).isEqualTo(RedisRateLimiter.Result.DENIED);
        verify(redisTemplate).expire(KEY, WINDOW);
    }

    @Test
    void redisErrorIsUnavailableForAcquireButAllowedForTryAcquire() {
        when(valueOperations.increment(KEY)).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(rateLimiter.acquire("test", 2, WINDOW)).isEqualTo(RedisRateLimiter.Result.UNAVAILABLE);
        assertThat(rateLimiter.tryAcquire("test", 2, WINDOW)).isTrue();
    }
}
