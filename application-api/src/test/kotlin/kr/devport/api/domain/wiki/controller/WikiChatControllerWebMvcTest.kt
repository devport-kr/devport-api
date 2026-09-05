@file:Suppress("ktlint:standard:max-line-length")

package kr.devport.api.domain.wiki.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletResponse
import kr.devport.api.domain.common.exception.GlobalExceptionHandler
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse
import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.service.WikiChatApplicationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

@ExtendWith(MockitoExtension::class)
class WikiChatControllerWebMvcTest {
    @Mock
    lateinit var chatApplicationService: WikiChatApplicationService

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean()
        validator.afterPropertiesSet()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(WikiChatController(chatApplicationService))
                .addFilters<StandaloneMockMvcBuilder>(
                    SecurityContextHolderFilter(HttpSessionSecurityContextRepository()),
                ).setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(GlobalExceptionHandler())
                .setValidator(validator)
                .build()
        objectMapper = ObjectMapper()
    }

    private fun authAs(): RequestPostProcessor {
        val details =
            CustomUserDetails(42L, "test@example.com", "testuser", null, "Test", listOf(SimpleGrantedAuthority("ROLE_USER")), null)
        return authentication(UsernamePasswordAuthenticationToken(details, null, details.authorities))
    }

    @Test
    @DisplayName("path chat endpoint keeps the compact answer-clarification-session contract")
    fun keepsCompactContract() {
        whenever(chatApplicationService.chatProject(any(), any(), any(), anyOrNull()))
            .thenReturn(
                WikiChatResponse.from(
                    WikiChatResult("인증은 JWT 필터 중심으로 동작해요.", true, listOf("로그인", "인가"), emptyList(), false, false),
                    "session-123",
                ),
            )

        val request = WikiChatRequest(question = "인증 구조가 뭐야?", sessionId = "session-123")

        mockMvc
            .perform(
                post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                    .with(authAs())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request)),
            ).andExpect(status().isOk())
            .andExpect(jsonPath("\$.answer").value("인증은 JWT 필터 중심으로 동작해요."))
            .andExpect(jsonPath("\$.isClarification").value(true))
            .andExpect(jsonPath("\$.sessionId").value("session-123"))
    }

    @Test
    @DisplayName("query-id chat endpoint reuses typed service clarification state")
    fun reusesTypedServiceClarificationState() {
        whenever(chatApplicationService.chatProject(any(), any(), any(), anyOrNull()))
            .thenReturn(
                WikiChatResponse.from(
                    WikiChatResult("요약하면 배포는 workflow 파일 기준으로 보면 돼요.", false, emptyList(), listOf("workflow 경로를 알려줘"), false, false),
                    "session-456",
                ),
            )

        val request = WikiChatRequest(question = "배포는 어디서 봐?", sessionId = "session-456")

        mockMvc
            .perform(
                post("/api/wiki/projects/chat")
                    .with(authAs())
                    .queryParam("id", "github:repo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request)),
            ).andExpect(status().isOk())
            .andExpect(jsonPath("\$.answer").value("요약하면 배포는 workflow 파일 기준으로 보면 돼요."))
            .andExpect(jsonPath("\$.isClarification").value(false))
            .andExpect(jsonPath("\$.sessionId").value("session-456"))
    }

    @Test
    @DisplayName("chat endpoint rejects blank question payloads")
    fun rejectsBlankQuestionPayloads() {
        val request = WikiChatRequest(question = " ", sessionId = "session-123")

        mockMvc
            .perform(
                post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request)),
            ).andExpect(status().isBadRequest())
    }

    @Test
    @DisplayName("unauthenticated chat requests return the compact Korean login message")
    fun unauthenticatedRequestReturnsKoreanLoginMessage() {
        val securedMockMvc =
            MockMvcBuilders
                .standaloneSetup(WikiChatController(chatApplicationService))
                .addFilters<StandaloneMockMvcBuilder>(
                    SecurityContextHolderFilter(HttpSessionSecurityContextRepository()),
                ).setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .addFilters<StandaloneMockMvcBuilder>(
                    Filter { _, response, _ ->
                        val httpResponse = response as HttpServletResponse
                        httpResponse.status = 401
                        httpResponse.contentType = "application/json;charset=UTF-8"
                        httpResponse.writer.write("{\"message\":\"챗봇과 대화할려면 로그인하세요\"}")
                    },
                ).build()

        val request = WikiChatRequest(question = "인증 구조가 뭐야?", sessionId = "session-123")

        securedMockMvc
            .perform(
                post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request)),
            ).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("\$.message").value("챗봇과 대화할려면 로그인하세요"))
    }

    @Test
    @DisplayName("session clear endpoint stays available as the fresh-start path")
    fun clearSessionReturnsNoContent() {
        mockMvc
            .perform(delete("/api/wiki/projects/{projectExternalId}/chat/sessions/{sessionId}", "github:repo", "session-123"))
            .andExpect(status().isNoContent())
    }

    @Test
    @DisplayName("streaming path endpoint returns text/event-stream and done event with sessionId")
    fun streamChatReturnsDoneEventWithSessionId() {
        doAnswer { invocation ->
            val consumer = invocation.getArgument<Consumer<String>>(4)
            consumer.accept("이")
            consumer.accept(" 프로젝트는")
            WikiChatResult("이 프로젝트는", false, emptyList(), emptyList(), false, false)
        }.whenever(chatApplicationService).streamProject(any(), any(), any(), anyOrNull(), any())

        val request = WikiChatRequest(question = "인증 구조가 뭐야?", sessionId = "session-789")

        val mvcResult =
            mockMvc
                .perform(
                    post("/api/wiki/projects/{projectExternalId}/chat/stream", "github:repo")
                        .with(authAs())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)),
                ).andReturn()

        // The doAnswer mock is synchronous; allow the virtual-thread executor to flush SSE events.
        Thread.sleep(200)

        assertThat(mvcResult.response.contentType).contains("text/event-stream")
        assertThat(mvcResult.response.contentAsString).contains("session-789")
    }

    @Test
    @DisplayName("streaming endpoint emits SSE error event when rate limit is exceeded")
    fun rateLimitedBeforeStreamEmitsErrorEvent() {
        doThrow(WikiChatRateLimitExceededException("요청이 너무 많습니다."))
            .whenever(chatApplicationService)
            .streamProject(any(), any(), any(), anyOrNull(), any())

        val request = WikiChatRequest(question = "인증 구조가 뭐야?", sessionId = "session-123")

        val mvcResult =
            mockMvc
                .perform(
                    post("/api/wiki/projects/{projectExternalId}/chat/stream", "github:repo")
                        .with(authAs())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)),
                ).andReturn()

        Thread.sleep(200)

        assertThat(mvcResult.response.contentType).contains("text/event-stream")
        assertThat(mvcResult.response.getContentAsString(StandardCharsets.UTF_8)).contains("요청이 너무 많습니다.")
    }
}
