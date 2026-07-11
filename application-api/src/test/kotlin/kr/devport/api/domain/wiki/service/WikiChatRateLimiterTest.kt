package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.infrastructure.RateLimitCounter
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class WikiChatRateLimiterTest {
    @Mock
    lateinit var rateLimitCounter: RateLimitCounter

    private lateinit var rateLimiter: WikiChatRateLimiter

    @BeforeEach
    fun setUp() {
        rateLimiter = WikiChatRateLimiter(rateLimitCounter)
    }

    @Test
    @DisplayName("check allows request when under per-minute limit")
    fun allowsRequestWhenUnderLimit() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(3L)
        whenever(rateLimitCounter.hit(DAILY_KEY, DAILY_WINDOW)).thenReturn(10L)

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check calls the counter with the minute and daily windows")
    fun callsCounterWithExpectedWindows() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(1L)
        whenever(rateLimitCounter.hit(DAILY_KEY, DAILY_WINDOW)).thenReturn(1L)

        rateLimiter.check("42")

        verify(rateLimitCounter).hit(eq(MINUTE_KEY), eq(MINUTE_WINDOW))
        verify(rateLimitCounter).hit(eq(DAILY_KEY), eq(DAILY_WINDOW))
    }

    @Test
    @DisplayName("check allows exactly the per-minute limit request")
    fun allowsExactlyLimitRequest() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(WikiChatRateLimiter.LIMIT.toLong())
        whenever(rateLimitCounter.hit(DAILY_KEY, DAILY_WINDOW)).thenReturn(1L)

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check throws with Korean burst message when per-minute limit exceeded")
    fun throwsOnRequestOverLimit() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(WikiChatRateLimiter.LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
    }

    @Test
    @DisplayName("check allows exactly the daily limit request")
    fun allowsExactlyDailyLimitRequest() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(1L)
        whenever(rateLimitCounter.hit(DAILY_KEY, DAILY_WINDOW)).thenReturn(WikiChatRateLimiter.DAILY_LIMIT.toLong())

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("check throws with Korean daily message when daily limit exceeded")
    fun throwsOnRequestOverDailyLimit() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(1L)
        whenever(rateLimitCounter.hit(DAILY_KEY, DAILY_WINDOW)).thenReturn(WikiChatRateLimiter.DAILY_LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("일일 요청 한도를 초과했습니다. 내일 다시 시도해 주세요.")
    }

    @Test
    @DisplayName("minute limit is checked before daily limit")
    fun minuteLimitBlocksBeforeDailyCounterIsReached() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(WikiChatRateLimiter.LIMIT.toLong() + 1)

        assertThatThrownBy { rateLimiter.check("42") }
            .isInstanceOf(WikiChatRateLimitExceededException::class.java)
            .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")

        verify(rateLimitCounter, never()).hit(eq(DAILY_KEY), eq(DAILY_WINDOW))
    }

    @Test
    @DisplayName("check allows request when the counter returns null")
    fun allowsWhenCounterReturnsNull() {
        whenever(rateLimitCounter.hit(MINUTE_KEY, MINUTE_WINDOW)).thenReturn(null)

        assertThatCode { rateLimiter.check("42") }.doesNotThrowAnyException()
    }

    companion object {
        private const val MINUTE_KEY = "wiki:rl:42"
        private const val DAILY_KEY = "wiki:rl:day:42"
        private val MINUTE_WINDOW = Duration.ofSeconds(60)
        private val DAILY_WINDOW = Duration.ofHours(24)
    }
}
