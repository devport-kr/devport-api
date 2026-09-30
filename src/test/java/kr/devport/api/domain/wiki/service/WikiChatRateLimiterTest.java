package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiChatRateLimiterTest {

    private static final String MINUTE_KEY = "wiki:rl:42";
    private static final String DAILY_KEY = "wiki:rl:day:42";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private WikiChatRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimiter = new WikiChatRateLimiter(redisTemplate);
    }

    // --- Per-minute window ---

    @Test
    @DisplayName("check allows request when under per-minute limit")
    void check_allowsRequestWhenUnderLimit() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(3L);

        assertThatCode(() -> rateLimiter.check("42")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("check sets 60-second expiry on first request in minute window")
    void check_setsExpiryOnFirstRequest() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(1L);

        rateLimiter.check("42");

        verify(redisTemplate).expire(eq(MINUTE_KEY), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("check does not reset minute-window expiry on subsequent requests")
    void check_doesNotResetExpiryOnSubsequentRequests() {
        // count=3: not first (no expiry reset), within limit (no exception)
        when(valueOps.increment(MINUTE_KEY)).thenReturn(3L);

        rateLimiter.check("42");

        verify(redisTemplate, never()).expire(eq(MINUTE_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("check allows exactly the per-minute limit request")
    void check_allowsExactlyLimitRequest() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn((long) WikiChatRateLimiter.LIMIT);

        assertThatCode(() -> rateLimiter.check("42")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("check throws with Korean burst message when per-minute limit exceeded")
    void check_throwsOnRequestOverLimit() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn((long) WikiChatRateLimiter.LIMIT + 1);

        assertThatThrownBy(() -> rateLimiter.check("42"))
                .isInstanceOf(WikiChatRateLimitExceededException.class)
                .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
    }

    // --- Per-day window ---

    @Test
    @DisplayName("check sets 24-hour expiry on first request in daily window")
    void check_setsExpiryOnFirstDailyRequest() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(1L);
        when(valueOps.increment(DAILY_KEY)).thenReturn(1L);

        rateLimiter.check("42");

        verify(redisTemplate).expire(eq(DAILY_KEY), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("check allows exactly the daily limit request")
    void check_allowsExactlyDailyLimitRequest() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(1L);
        when(valueOps.increment(DAILY_KEY)).thenReturn((long) WikiChatRateLimiter.DAILY_LIMIT);

        assertThatCode(() -> rateLimiter.check("42")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("check throws with Korean daily message when daily limit exceeded")
    void check_throwsOnRequestOverDailyLimit() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(1L);
        when(valueOps.increment(DAILY_KEY)).thenReturn((long) WikiChatRateLimiter.DAILY_LIMIT + 1);

        assertThatThrownBy(() -> rateLimiter.check("42"))
                .isInstanceOf(WikiChatRateLimitExceededException.class)
                .hasMessage("일일 요청 한도를 초과했습니다. 내일 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("check does not reset daily-window expiry on subsequent daily requests")
    void check_doesNotResetDailyExpiryOnSubsequentRequests() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(1L);
        // count=50: not first daily request (no expiry reset), within daily limit
        when(valueOps.increment(DAILY_KEY)).thenReturn(50L);

        rateLimiter.check("42");

        verify(redisTemplate, never()).expire(eq(DAILY_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("minute limit is checked before daily limit — daily counter not incremented on burst rejection")
    void check_minuteLimitBlocksBeforeDailyCounterIsReached() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn((long) WikiChatRateLimiter.LIMIT + 1);

        assertThatThrownBy(() -> rateLimiter.check("42"))
                .isInstanceOf(WikiChatRateLimitExceededException.class)
                .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");

        verify(valueOps, never()).increment(DAILY_KEY);
    }

    // --- Fail-open behaviour ---

    @Test
    @DisplayName("check allows request when Redis returns null (fail-open)")
    void check_allowsWhenRedisReturnsNull() {
        when(valueOps.increment(MINUTE_KEY)).thenReturn(null);

        assertThatCode(() -> rateLimiter.check("42")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("check allows request when Redis throws an exception (fail-open)")
    void check_allowsWhenRedisThrows() {
        when(valueOps.increment(MINUTE_KEY)).thenThrow(new RuntimeException("Redis connection refused"));

        assertThatCode(() -> rateLimiter.check("42")).doesNotThrowAnyException();
    }
}
