package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.service.WikiChatApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiChatControllerTest {

    @Mock
    private WikiChatApplicationService chatApplicationService;

    @InjectMocks
    private WikiChatController wikiChatController;

    private CustomUserDetails testUser() {
        return new CustomUserDetails(42L, "test@example.com", "testuser", null, "Test",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), null);
    }

    @Test
    @DisplayName("chat response no longer contains citation payload")
    void chat_withoutCitationField() {
        boolean hasCitationField = Arrays.stream(WikiChatResponse.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("citations"));

        assertThat(hasCitationField).isFalse();
    }

    @Test
    @DisplayName("chat endpoint maps typed service results directly")
    void chat_mapsTypedServiceResultsDirectly() {
        String projectExternalId = "github:12345";
        WikiChatResult serviceResult = new WikiChatResult(
                "요약하면 인증은 JwtAuthenticationFilter 중심이에요.",
                true,
                java.util.List.of("로그인", "인가"),
                java.util.List.of(),
                false,
                false
        );

        when(chatApplicationService.chatProject(eq(projectExternalId), any(), eq(42L), isNull()))
                .thenReturn(WikiChatResponse.from(serviceResult, "session-123"));

        WikiChatRequest request = WikiChatRequest.builder()
                .question("How does auth work?")
                .sessionId("session-123")
                .build();

        ResponseEntity<WikiChatResponse> response = wikiChatController.chat(
                projectExternalId,
                request,
                testUser(),
                new MockHttpServletRequest()
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo(serviceResult.answer());
        assertThat(response.getBody().getSessionId()).isEqualTo("session-123");
        assertThat(response.getBody().isClarification()).isTrue();

        verify(chatApplicationService).chatProject(eq(projectExternalId), eq(request), eq(42L), isNull());
    }

    @Test
    @DisplayName("query-id chat endpoint preserves compact contract")
    void chatByQueryId_preservesCompactContract() {
        WikiChatResult serviceResult = new WikiChatResult(
                "요약하면 배포는 workflow 파일 기준으로 보는 게 좋아요.",
                false,
                java.util.List.of(),
                java.util.List.of("deploy workflow 경로를 알려줘"),
                false,
                false
        );

        when(chatApplicationService.chatProject(eq("github:12345"), any(), eq(42L), isNull()))
                .thenReturn(WikiChatResponse.from(serviceResult, "session-123"));

        WikiChatRequest request = WikiChatRequest.builder()
                .question("배포는 어디서 봐?")
                .sessionId("session-123")
                .build();

        ResponseEntity<WikiChatResponse> response = wikiChatController.chatByQueryId(
                "github:12345",
                request,
                testUser(),
                new MockHttpServletRequest()
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo(serviceResult.answer());
        assertThat(response.getBody().isClarification()).isFalse();
        assertThat(response.getBody().getSessionId()).isEqualTo("session-123");

        verify(chatApplicationService).chatProject(eq("github:12345"), eq(request), eq(42L), isNull());
    }

    @Test
    @DisplayName("clearSession delegates to service")
    void clearSession_delegatesToService() {
        // Given
        String projectExternalId = "github:12345";
        String sessionId = "session-123";

        // When
        ResponseEntity<Void> response = wikiChatController.clearSession(projectExternalId, sessionId);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(chatApplicationService).clearProjectSession(sessionId);
    }
}
