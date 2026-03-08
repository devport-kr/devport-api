package kr.devport.api.domain.wiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.service.WikiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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
    @DisplayName("chat endpoint keeps the compact answer-clarification-session contract")
    void chat_keepsCompactContract() throws Exception {
        when(wikiChatService.chat(anyString(), anyString(), anyString()))
                .thenReturn("인증은 JWT 필터 중심으로 동작해요.");

        WikiChatRequest request = WikiChatRequest.builder()
                .question("인증 구조가 뭐야?")
                .sessionId("session-123")
                .build();

        mockMvc.perform(post("/api/wiki/projects/{projectExternalId}/chat", "github:repo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("인증은 JWT 필터 중심으로 동작해요."))
                .andExpect(jsonPath("$.isClarification").value(false))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
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

    @Disabled("Enable in 06-04 when a security-backed MVC slice is added for auth behavior")
    @Test
    @DisplayName("unauthenticated chat requests return the compact Korean login message")
    void chat_unauthenticatedRequestReturnsKoreanLoginMessage() {
    }
}
