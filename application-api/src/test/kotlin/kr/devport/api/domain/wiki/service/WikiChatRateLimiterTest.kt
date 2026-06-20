package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class WikiChatRateLimiterTest {
    @Mock
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    lateinit var valueOps: ValueOperations<String, Any>

    private lateinit var rateLimiter: WikiChatRateLimiter

    @BeforeEach
    fun setUp() {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        rateLimiter = WikiChatRateLimiter(redisTemplate)
    }

    @Test
    @DisplayName("check allows request when under per-minute limit")
    fun allowsRequestWhenUnderLimit() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(3L)

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check sets 60-second expiry on first request in minute window")
    fun setsExpiryOnFirstRequest() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(1L)

        rateLimiter.check("42")

        verify(redisTemplate).expire(eq(MINUTE_KEY), eq(Duration.ofSeconds(60)))
    }

    @Test
    @DisplayName("check does not reset minute-window expiry on subsequent requests")
    fun doesNotResetExpiryOnSubsequentRequests() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(3L)

        rateLimiter.check("42")

        verify(redisTemplate, never()).expire(eq(MINUTE_KEY), any<Duration>())
    }

    @Test
    @DisplayName("check allows exactly the per-minute limit request")
    fun allowsExactlyLimitRequest() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(WikiChatRateLimiter.LIMIT.toLong())

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check throws with Korean burst message when per-minute limit exceeded")
    fun throwsOnRequestOverLimit() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(WikiChatRateLimiter.LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
    }

    @Test
    @DisplayName("check sets 24-hour expiry on first request in daily window")
    fun setsExpiryOnFirstDailyRequest() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(1L)
        whenever(valueOps.increment(DAILY_KEY)).thenReturn(1L)

        rateLimiter.check("42")

        verify(redisTemplate).expire(eq(DAILY_KEY), eq(Duration.ofHours(24)))
    }

    @Test
    @DisplayName("check allows exactly the daily limit request")
    fun allowsExactlyDailyLimitRequest() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(1L)
        whenever(valueOps.increment(DAILY_KEY)).thenReturn(WikiChatRateLimiter.DAILY_LIMIT.toLong())

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check throws with Korean daily message when daily limit exceeded")
    fun throwsOnRequestOverDailyLimit() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(1L)
        whenever(valueOps.increment(DAILY_KEY)).thenReturn(WikiChatRateLimiter.DAILY_LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("일일 요청 한도를 초과했습니다. 내일 다시 시도해 주세요.")
    }

    @Test
    @DisplayName("check does not reset daily-window expiry on subsequent daily requests")
    fun doesNotResetDailyExpiryOnSubsequentRequests() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(1L)
        whenever(valueOps.increment(DAILY_KEY)).thenReturn(50L)

        rateLimiter.check("42")

        verify(redisTemplate, never()).expire(eq(DAILY_KEY), any<Duration>())
    }

    @Test
    @DisplayName("minute limit is checked before daily limit — daily counter not incremented on burst rejection")
    fun minuteLimitBlocksBeforeDailyCounterIsReached() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(WikiChatRateLimiter.LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")

        verify(valueOps, never()).increment(DAILY_KEY)
    }

    @Test
    @DisplayName("check allows request when Redis returns null (fail-open)")
    fun allowsWhenRedisReturnsNull() {
        whenever(valueOps.increment(MINUTE_KEY)).thenReturn(null)

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check allows request when Redis throws an exception (fail-open)")
    fun allowsWhenRedisThrows() {
        whenever(valueOps.increment(MINUTE_KEY)).thenThrow(RuntimeException("Redis connection refused"))

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    companion object {
        private const val MINUTE_KEY = "wiki:rl:42"
        private const val DAILY_KEY = "wiki:rl:day:42"
    }
}
