package kr.devport.api.domain.wiki.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WikiChatSessionStoreTest {
    @Mock
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    lateinit var valueOperations: ValueOperations<String, Any>

    private lateinit var sessionStore: WikiChatSessionStore
    private val redisStore = mutableMapOf<String, Any>()

    @BeforeEach
    fun setUp() {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)

        doAnswer { invocation ->
            redisStore[invocation.getArgument(0)] = invocation.getArgument(1)
            null
        }.whenever(valueOperations).set(any<String>(), any(), any<Duration>())

        whenever(valueOperations.get(any<String>())).thenAnswer { invocation -> redisStore[invocation.getArgument<String>(0)] }

        whenever(redisTemplate.delete(any<String>())).thenAnswer { invocation ->
            redisStore.remove(invocation.getArgument<String>(0)) != null
        }

        whenever(redisTemplate.hasKey(any<String>())).thenAnswer { invocation ->
            redisStore.containsKey(invocation.getArgument<String>(0))
        }

        sessionStore = WikiChatSessionStore(redisTemplate)
    }

    @Test
    @DisplayName("saveTurn keeps recent turns available for the active session")
    fun saveTurnKeepsRecentTurnsAvailableForActiveSession() {
        sessionStore.saveTurn("session-1", "github:repo", "auth?", "JWT를 사용해요.", false)

        val turns = sessionStore.loadTurns("session-1", "github:repo")

        assertThat(turns).hasSize(1)
        assertThat(turns.first().question).isEqualTo("auth?")
        assertThat(turns.first().answer).isEqualTo("JWT를 사용해요.")
        assertThat(sessionStore.hasActiveSession("session-1")).isTrue()
    }

    @Test
    @DisplayName("saveTurn prunes oldest turns after the session reaches capacity")
    fun saveTurnPrunesOldestTurnsAfterSessionReachesCapacity() {
        for (index in 1..11) {
            sessionStore.saveTurn("session-2", "github:repo", "q$index", "a$index", false)
        }

        val turns = sessionStore.loadTurns("session-2", "github:repo")

        assertThat(turns).hasSize(10)
        assertThat(turns.first().question).isEqualTo("q2")
        assertThat(turns.last().question).isEqualTo("q11")
    }

    @Test
    @DisplayName("reusing a session ID on another project resets stale memory")
    fun saveTurnResetsStaleMemoryWhenProjectChanges() {
        sessionStore.saveTurn("shared-session", "github:repo-a", "A 질문", "A 답변", false)
        sessionStore.saveTurn("shared-session", "github:repo-b", "B 질문", "B 답변", false)

        val turns = sessionStore.loadTurns("shared-session", "github:repo-b")

        assertThat(turns.map { it.question }).containsExactly("B 질문")
        assertThat(sessionStore.loadTurns("shared-session", "github:repo-a")).isEmpty()
    }

    @Test
    @DisplayName("recent-turn selection returns only the last three turns for prompt assembly")
    fun loadRecentTurnsReturnsOnlyLastThreeTurns() {
        sessionStore.saveTurn("session-3", "github:repo", "빌드 질문", "빌드 답변", false)
        sessionStore.saveTurn("session-3", "github:repo", "배포 질문", "배포 답변", false)
        sessionStore.saveTurn("session-3", "github:repo", "테스트 질문", "테스트 답변", false)
        sessionStore.saveTurn("session-3", "github:repo", "아키텍처 질문", "아키텍처 답변", false)

        val turns = sessionStore.loadRecentTurns("session-3", "github:repo")

        assertThat(turns.map { it.question }).containsExactly("배포 질문", "테스트 질문", "아키텍처 질문")
    }

    @Test
    @DisplayName("saveTurn refreshes ttl and clearSession removes the session immediately")
    fun saveTurnRefreshesTtlAndClearSessionRemovesSession() {
        sessionStore.saveTurn("session-4", "github:repo", "첫 질문", "첫 답변", false)
        val initialExpiry = sessionStore.getSession("session-4")!!.expiresAt

        sessionStore.saveTurn("session-4", "github:repo", "두 번째 질문", "두 번째 답변", true)

        val refreshedExpiry = sessionStore.getSession("session-4")!!.expiresAt
        assertThat(refreshedExpiry).isAfter(initialExpiry)

        sessionStore.clearSession("session-4")

        assertThat(sessionStore.hasActiveSession("session-4")).isFalse()
        assertThat(sessionStore.loadTurns("session-4", "github:repo")).isEmpty()
    }
}
