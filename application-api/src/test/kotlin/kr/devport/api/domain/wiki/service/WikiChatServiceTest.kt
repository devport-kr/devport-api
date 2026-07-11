@file:Suppress("ktlint:standard:max-line-length")

package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk
import kr.devport.api.domain.wiki.infrastructure.ChatMessage
import kr.devport.api.domain.wiki.infrastructure.ChatPort
import kr.devport.api.domain.wiki.infrastructure.ChatTurn
import kr.devport.api.domain.wiki.infrastructure.WikiChatSessionStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WikiChatServiceTest {
    @Mock
    lateinit var retrievalService: WikiRetrievalService

    @Mock
    lateinit var sessionStore: WikiChatSessionStore

    @Mock
    lateinit var persistenceService: WikiChatSessionPersistenceService

    @Mock
    lateinit var titleService: WikiChatTitleService

    @Mock
    lateinit var chatPort: ChatPort

    @InjectMocks
    lateinit var wikiChatService: WikiChatService

    @BeforeEach
    fun setUp() {
        stubChatJson(
            """
            {"answer":"요약하면 인증 진입점은 SecurityConfig와 JwtAuthenticationFilter예요.","isClarification":false,"clarificationOptions":[],"suggestedNextQuestions":[],"usedPreviousContext":false}
            """.trimIndent(),
        )
    }

    private fun stubChatJson(json: String) {
        whenever(chatPort.complete(any(), any(), anyOrNull(), anyOrNull())).thenReturn(json)
    }

    @Test
    @DisplayName("chat keeps only the most recent relevant turns in the model prompt")
    fun keepsOnlyRecentRelevantTurnsInPrompt() {
        whenever(retrievalService.retrieveContext("github:repo", "JWT 흐름 설명해줘")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("session-123", "github:repo")).thenReturn(
            listOf(
                turn("아주 예전 질문 1", "예전 답변 1", false),
                turn("아주 예전 질문 2", "예전 답변 2", false),
                turn("아주 예전 질문 3", "예전 답변 3", false),
                turn("아주 예전 질문 4", "예전 답변 4", false),
                turn("아주 예전 질문 5", "예전 답변 5", false),
                turn("아주 예전 질문 6", "예전 답변 6", false),
                turn("아주 예전 질문 7", "예전 답변 7", false),
                turn("아주 예전 질문 8", "예전 답변 8", false),
                turn("아주 예전 질문 9", "예전 답변 9", false),
                turn("아주 예전 질문", "예전 답변", false),
                turn("로그인 흐름이 어디서 시작돼?", "SecurityConfig에서 시작돼요.", false),
                turn("그다음 JWT 검사는?", "JwtAuthenticationFilter가 담당해요.", false),
            ),
        )
        whenever(sessionStore.hasActiveSession("session-123")).thenReturn(true)

        val result = wikiChatService.chatResult("session-123", "github:repo", "JWT 흐름 설명해줘")

        val messagesText = captureCompletedMessagesText()

        assertThat(result.isClarification).isFalse()
        assertThat(messagesText).contains("로그인 흐름이 어디서 시작돼?")
        assertThat(messagesText).contains("그다음 JWT 검사는?")
        assertThat(messagesText).doesNotContain("아주 예전 질문 1")
        assertThat(messagesText).doesNotContain("아주 예전 질문 2")
    }

    @Test
    @DisplayName("chat drops stale memory on topic shift and keeps weak-grounding replies short")
    fun dropsStaleMemoryOnTopicShiftAndKeepsWeakGroundingRepliesShort() {
        whenever(retrievalService.retrieveContext("github:repo", "배포 파이프라인이 어디에 있어?")).thenReturn(
            WikiRetrievalContext(
                "github:repo",
                "# Repository Context\n\n## CI\n.github/workflows/deploy.yml",
                true,
                true,
                listOf(
                    WikiRetrievedChunk("ci", null, "body", "CI", ".github/workflows/deploy.yml", 0.4, 0.2, null, ".github/workflows/deploy.yml"),
                ),
                listOf("deploy.yml 기준으로 단계별로 설명해줘", "배포에 쓰는 secret 목록을 알려줘", "실패 시 롤백 흐름을 알려줘"),
            ),
        )
        whenever(sessionStore.loadRecentTurns("session-123", "github:repo")).thenReturn(
            listOf(
                turn("JWT 구조 알려줘", "인증 필터 중심이에요.", false),
                turn("리프레시 토큰은?", "재발급 API에서 처리해요.", false),
            ),
        )
        whenever(sessionStore.hasActiveSession("session-123")).thenReturn(true)
        stubChatJson(
            """
            {"answer":"요약하면 배포 정보는 CI 설정 위주로만 확인돼요.","isClarification":false,"clarificationOptions":[],"suggestedNextQuestions":["deploy.yml 기준으로 단계별로 설명해줘","배포에 쓰는 secret 목록을 알려줘","실패 시 롤백 흐름을 알려줘"],"usedPreviousContext":true}
            """.trimIndent(),
        )

        val result = wikiChatService.chatResult("session-123", "github:repo", "배포 파이프라인이 어디에 있어?")

        assertThat(result.isClarification).isFalse()
        assertThat(result.usedPreviousContext).isFalse()
        assertThat(result.suggestedNextQuestions).hasSize(3)
        assertThat(result.answer).startsWith("요약하면")
        assertThat(result.answer).contains("다음처럼 좁혀서 물어보면 더 정확해요:")
        assertThat(result.answer).contains("deploy.yml 기준으로 단계별로 설명해줘")
    }

    @Test
    @DisplayName("chat inlines clarification options into compact responses when clarification is needed")
    fun inlinesClarificationOptionsIntoCompactResponses() {
        whenever(retrievalService.retrieveContext("github:repo", "인증 쪽 설명해줘")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("session-123", "github:repo")).thenReturn(emptyList())
        whenever(sessionStore.hasActiveSession("session-123")).thenReturn(false)
        stubChatJson(
            """
            {"answer":"요약: 질문이 모호합니다. 아래 옵션 중 하나를 골라 주세요.","isClarification":true,"clarificationOptions":["로그인","인가","토큰 갱신"],"suggestedNextQuestions":[],"usedPreviousContext":false}
            """.trimIndent(),
        )

        val result = wikiChatService.chatResult("session-123", "github:repo", "인증 쪽 설명해줘")

        assertThat(result.isClarification).isTrue()
        assertThat(result.clarificationOptions).containsExactly("로그인", "인가", "토큰 갱신")
        assertThat(result.answer).contains("선택할 수 있는 범위:")
        assertThat(result.answer).contains("- 로그인")
        assertThat(result.answer).contains("- 인가")
        assertThat(result.answer).contains("- 토큰 갱신")
    }

    @Test
    @DisplayName("isTopicShift returns false for Korean meta-history questions")
    fun returnsFalseForKoreanMetaHistory() {
        whenever(retrievalService.retrieveContext("github:repo", "이전 질문 요약해줘")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("s1", "github:repo"))
            .thenReturn(listOf(turn("JWT 구조 알려줘", "JWT는 헤더/페이로드/서명이에요.", false)))
        whenever(sessionStore.hasActiveSession("s1")).thenReturn(true)

        wikiChatService.chatResult("s1", "github:repo", "이전 질문 요약해줘")

        assertThat(captureCompletedMessagesText()).contains("JWT 구조 알려줘")
    }

    @Test
    @DisplayName("isTopicShift returns false for English meta-history questions")
    fun returnsFalseForEnglishMetaHistory() {
        whenever(retrievalService.retrieveContext("github:repo", "what did I ask before?")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("s2", "github:repo"))
            .thenReturn(listOf(turn("How does JWT work?", "JWT는 헤더/페이로드/서명이에요.", false)))
        whenever(sessionStore.hasActiveSession("s2")).thenReturn(true)

        wikiChatService.chatResult("s2", "github:repo", "what did I ask before?")

        assertThat(captureCompletedMessagesText()).contains("How does JWT work?")
    }

    @Test
    @DisplayName("isTopicShift returns false for short follow-up questions with under 2 tokens")
    fun returnsFalseForShortFollowUpQuestion() {
        whenever(retrievalService.retrieveContext("github:repo", "왜?")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("s3", "github:repo"))
            .thenReturn(listOf(turn("SecurityConfig 역할이 뭐야?", "보안 설정 진입점이에요.", false)))
        whenever(sessionStore.hasActiveSession("s3")).thenReturn(true)

        wikiChatService.chatResult("s3", "github:repo", "왜?")

        assertThat(captureCompletedMessagesText()).contains("SecurityConfig 역할이 뭐야?")
    }

    @Test
    @DisplayName("clearSession delegates to session store")
    fun clearSessionDelegatesToStore() {
        wikiChatService.clearSession("session-123")

        verify(sessionStore).clearSession("session-123")
    }

    @Test
    @DisplayName("hasActiveSession delegates to session store")
    fun hasActiveSessionDelegatesToStore() {
        whenever(sessionStore.hasActiveSession("session-123")).thenReturn(true)

        val hasActive = wikiChatService.hasActiveSession("session-123")

        assertThat(hasActive).isTrue()
        verify(sessionStore).hasActiveSession("session-123")
    }

    @Test
    @DisplayName("streamChatResult calls token consumer for each streamed token and saves the session turn")
    fun callsTokenConsumerAndSavesSession() {
        whenever(retrievalService.retrieveContext("github:repo", "인증 구조가 뭐야?")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("session-s", "github:repo")).thenReturn(emptyList())
        whenever(sessionStore.hasActiveSession("session-s")).thenReturn(false)
        stubStreamTokens("요약하면 ", "인증 필터 중심이에요.")

        val received = mutableListOf<String>()
        val result = wikiChatService.streamChatResult("session-s", "github:repo", "인증 구조가 뭐야?", received::add)

        assertThat(received).containsExactly("요약하면 ", "인증 필터 중심이에요.")
        assertThat(result.answer).contains("요약하면")
        assertThat(result.isClarification).isFalse()
        verify(sessionStore).saveTurn(any(), any(), any(), any(), any<Boolean>())
    }

    @Test
    @DisplayName("streamChatResult detects isClarification=true from clarification heading in accumulated text")
    fun detectsClarificationFromHeading() {
        whenever(retrievalService.retrieveContext("github:repo", "인증 쪽 설명해줘")).thenReturn(strongContext())
        whenever(sessionStore.loadRecentTurns("session-c", "github:repo")).thenReturn(emptyList())
        whenever(sessionStore.hasActiveSession("session-c")).thenReturn(false)

        val clarificationText = "질문이 모호해요.\n\n선택할 수 있는 범위:\n- 로그인\n- 인가\n- 토큰 갱신"
        stubStreamTokens(clarificationText)

        val result = wikiChatService.streamChatResult("session-c", "github:repo", "인증 쪽 설명해줘", { _ -> })

        assertThat(result.isClarification).isTrue()
        assertThat(result.clarificationOptions).containsExactly("로그인", "인가", "토큰 갱신")
    }

    private fun captureCompletedMessagesText(): String {
        val captor = argumentCaptor<List<ChatMessage>>()
        verify(chatPort, atLeastOnce()).complete(any(), captor.capture(), anyOrNull(), anyOrNull())
        return captor.firstValue.joinToString("\n") { it.content }
    }

    private fun stubStreamTokens(vararg tokens: String) {
        doAnswer { invocation ->
            val onToken = invocation.getArgument<(String) -> Unit>(2)
            tokens.forEach(onToken)
            Unit
        }.whenever(chatPort).stream(eq("gpt-5-mini"), any(), any())
    }

    private fun strongContext(): WikiRetrievalContext =
        WikiRetrievalContext(
            "github:repo",
            "# Repository Context\n\n## 인증 흐름\nSecurityConfig -> JwtAuthenticationFilter",
            true,
            false,
            listOf(
                WikiRetrievedChunk(
                    "architecture",
                    "auth",
                    "body",
                    "인증 흐름",
                    "SecurityConfig -> JwtAuthenticationFilter",
                    0.9,
                    0.7,
                    null,
                    "src/main/java/kr/devport/api/domain/common/config/SecurityConfig.java",
                ),
            ),
            emptyList(),
        )

    private fun turn(
        question: String,
        answer: String,
        clarification: Boolean,
    ): ChatTurn = ChatTurn(question = question, answer = answer, wasClarification = clarification)
}
