package kr.devport.api.domain.wiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.service.WikiChatRateLimiter;
import kr.devport.api.domain.wiki.service.WikiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Consumer;
import kr.devport.api.domain.common.exception.GlobalExceptionHandler;
import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WikiChatControllerWebMvcTest {

    @Mock
    private WikiChatService wikiChatService;

    @Mock
    private WikiChatRateLimiter rateLimiter;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new WikiChatController(wikiChatService, rateLimiter))
                .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    private RequestPostProcessor authAs() {
        CustomUserDetails details = new CustomUserDetails(42L, "test@example.com", "testuser",
                null, "Test", List.of(new SimpleGrantedAuthority("ROLE_USER")), null);
        return authentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("path chat endpoint keeps the compact answer-clarification-session contract")
    void chat_keepsCompactContract() throws Exception {
        when(wikiChatService.chatResult("session-123", "github:repo", "인증 구조가 뭐야?"))
                .thenReturn(new WikiChatResult(
                        "인증은 JWT 필터 중심으로 동작해요.",
                        true,
                        java.util.List.of("로그인", "인가"),
                        java.util.List.of(),
                        false,
                        false
                ));

        WikiChatRequest request = WikiChatRequest.builder()
                .question("인증 구조가 뭐야?")
                .sessionId("session-123")
                .build();

        mockMvc.perform(post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                        .with(authAs())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("인증은 JWT 필터 중심으로 동작해요."))
                .andExpect(jsonPath("$.isClarification").value(true))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
    }

    @Test
    @DisplayName("query-id chat endpoint reuses typed service clarification state")
    void chatByQueryId_reusesTypedServiceClarificationState() throws Exception {
        when(wikiChatService.chatResult("session-456", "github:repo", "배포는 어디서 봐?"))
                .thenReturn(new WikiChatResult(
                        "요약하면 배포는 workflow 파일 기준으로 보면 돼요.",
                        false,
                        java.util.List.of(),
                        java.util.List.of("workflow 경로를 알려줘"),
                        false,
                        false
                ));

        WikiChatRequest request = WikiChatRequest.builder()
                .question("배포는 어디서 봐?")
                .sessionId("session-456")
                .build();

        mockMvc.perform(post("/api/wiki/projects/chat")
                        .with(authAs())
                        .queryParam("id", "github:repo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("요약하면 배포는 workflow 파일 기준으로 보면 돼요."))
                .andExpect(jsonPath("$.isClarification").value(false))
                .andExpect(jsonPath("$.sessionId").value("session-456"));
    }

    @Test
    @DisplayName("chat endpoint rejects blank question payloads")
    void chat_rejectsBlankQuestionPayloads() throws Exception {
        WikiChatRequest request = WikiChatRequest.builder()
                .question(" ")
                .sessionId("session-123")
                .build();

        mockMvc.perform(post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("unauthenticated chat requests return the compact Korean login message")
    void chat_unauthenticatedRequestReturnsKoreanLoginMessage() throws Exception {
        MockMvc securedMockMvc = MockMvcBuilders.standaloneSetup(new WikiChatController(wikiChatService, rateLimiter))
                .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilter((request, response, chain) -> {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(401);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write("{\"message\":\"챗봇과 대화할려면 로그인하세요\"}");
                })
                .build();

        WikiChatRequest request = WikiChatRequest.builder()
                .question("인증 구조가 뭐야?")
                .sessionId("session-123")
                .build();

        securedMockMvc.perform(post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("챗봇과 대화할려면 로그인하세요"));
    }

    @Test
    @DisplayName("session clear endpoint stays available as the fresh-start path")
    void clearSession_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/wiki/projects/{projectExternalId}/chat/sessions/{sessionId}", "github:repo", "session-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("streaming path endpoint returns text/event-stream and done event with sessionId")
    void streamChat_returnsDoneEventWithSessionId() throws Exception {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> consumer = invocation.getArgument(3);
            consumer.accept("이");
            consumer.accept(" 프로젝트는");
            return new WikiChatResult("이 프로젝트는", false, java.util.List.of(), java.util.List.of(), false, false);
        }).when(wikiChatService).streamChatResult(any(), any(), any(), any());

        WikiChatRequest request = WikiChatRequest.builder()
                .question("인증 구조가 뭐야?")
                .sessionId("session-789")
                .build();

        MvcResult mvcResult = mockMvc.perform(
                        post("/api/wiki/projects/{projectExternalId}/chat/stream", "github:repo")
                                .with(authAs())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request)))
                .andReturn();

        // Give the virtual-thread executor time to write SSE events and complete the emitter.
        // The doAnswer mock is synchronous so this takes microseconds; 200 ms is generous.
        Thread.sleep(200);

        assertThat(mvcResult.getResponse().getContentType()).contains("text/event-stream");
        assertThat(mvcResult.getResponse().getContentAsString()).contains("session-789");
    }

    @Test
    @DisplayName("streaming endpoint returns 429 synchronously when rate limit is exceeded before stream starts")
    void streamChat_rateLimitedBeforeStreamReturns429() throws Exception {
        doThrow(new WikiChatRateLimitExceededException("요청이 너무 많습니다."))
                .when(rateLimiter).check(any());

        WikiChatRequest request = WikiChatRequest.builder()
                .question("인증 구조가 뭐야?")
                .sessionId("session-123")
                .build();

        mockMvc.perform(post("/api/wiki/projects/{projectExternalId}/chat/stream", "github:repo")
                        .with(authAs())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isTooManyRequests());
    }
}
