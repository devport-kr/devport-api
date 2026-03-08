package kr.devport.api.domain.wiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.service.WikiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WikiChatControllerWebMvcTest {

    @Mock
    private WikiChatService wikiChatService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new WikiChatController(wikiChatService))
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
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
        MockMvc securedMockMvc = MockMvcBuilders.standaloneSetup(new WikiChatController(wikiChatService))
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
}
