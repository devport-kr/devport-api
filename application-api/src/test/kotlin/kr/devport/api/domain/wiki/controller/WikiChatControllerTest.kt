package kr.devport.api.domain.wiki.controller

import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse
import kr.devport.api.domain.wiki.service.WikiChatApplicationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority

@ExtendWith(MockitoExtension::class)
class WikiChatControllerTest {
    @Mock
    lateinit var chatApplicationService: WikiChatApplicationService

    @InjectMocks
    lateinit var wikiChatController: WikiChatController

    private fun testUser(): CustomUserDetails =
        CustomUserDetails(
            42L,
            "test@example.com",
            "testuser",
            null,
            "Test",
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            null,
        )

    @Test
    @DisplayName("chat response no longer contains citation payload")
    fun chatWithoutCitationField() {
        val hasCitationField = WikiChatResponse::class.java.declaredFields.any { it.name == "citations" }

        assertThat(hasCitationField).isFalse()
    }

    @Test
    @DisplayName("chat endpoint maps typed service results directly")
    fun chatMapsTypedServiceResultsDirectly() {
        val projectExternalId = "github:12345"
        val serviceResult =
            WikiChatResult(
                "요약하면 인증은 JwtAuthenticationFilter 중심이에요.",
                true,
                listOf("로그인", "인가"),
                emptyList(),
                false,
                false,
            )

        whenever(chatApplicationService.chatProject(eq(projectExternalId), any(), eq(42L), isNull()))
            .thenReturn(WikiChatResponse.from(serviceResult, "session-123"))

        val request = WikiChatRequest(question = "How does auth work?", sessionId = "session-123")

        val response = wikiChatController.chat(projectExternalId, request, testUser(), MockHttpServletRequest())

        assertThat(response.body).isNotNull()
        assertThat(response.body!!.answer).isEqualTo(serviceResult.answer)
        assertThat(response.body!!.sessionId).isEqualTo("session-123")
        assertThat(response.body!!.isClarification).isTrue()

        verify(chatApplicationService).chatProject(eq(projectExternalId), eq(request), eq(42L), isNull())
    }

    @Test
    @DisplayName("query-id chat endpoint preserves compact contract")
    fun chatByQueryIdPreservesCompactContract() {
        val serviceResult =
            WikiChatResult(
                "요약하면 배포는 workflow 파일 기준으로 보는 게 좋아요.",
                false,
                emptyList(),
                listOf("deploy workflow 경로를 알려줘"),
                false,
                false,
            )

        whenever(chatApplicationService.chatProject(eq("github:12345"), any(), eq(42L), isNull()))
            .thenReturn(WikiChatResponse.from(serviceResult, "session-123"))

        val request = WikiChatRequest(question = "배포는 어디서 봐?", sessionId = "session-123")

        val response = wikiChatController.chatByQueryId("github:12345", request, testUser(), MockHttpServletRequest())

        assertThat(response.body).isNotNull()
        assertThat(response.body!!.answer).isEqualTo(serviceResult.answer)
        assertThat(response.body!!.isClarification).isFalse()
        assertThat(response.body!!.sessionId).isEqualTo("session-123")

        verify(chatApplicationService).chatProject(eq("github:12345"), eq(request), eq(42L), isNull())
    }

    @Test
    @DisplayName("clearSession delegates to service")
    fun clearSessionDelegatesToService() {
        val projectExternalId = "github:12345"
        val sessionId = "session-123"

        val response = wikiChatController.clearSession(projectExternalId, sessionId)

        assertThat(response.statusCode.is2xxSuccessful()).isTrue()
        verify(chatApplicationService).clearProjectSession(sessionId)
    }
}
