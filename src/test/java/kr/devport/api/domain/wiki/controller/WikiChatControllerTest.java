package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.service.WikiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiChatControllerTest {

    @Mock
    private WikiChatService wikiChatService;

    @InjectMocks
    private WikiChatController wikiChatController;

    @Test
    @DisplayName("chat endpoint returns precise answer with session tracking")
    void chat_returnsPreciseAnswerWithSessionTracking() {
        // Given
        String projectExternalId = "github:12345";
        String answer = "This project uses JWT authentication.";
        
        when(wikiChatService.chat(anyString(), anyString(), anyString()))
                .thenReturn(answer);

        WikiChatRequest request = WikiChatRequest.builder()
                .question("How does auth work?")
                .sessionId("session-123")
                .includeCitations(false)
                .build();

        // When
        ResponseEntity<WikiChatResponse> response = wikiChatController.chat(projectExternalId, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isEqualTo(answer);
        assertThat(response.getBody().getSessionId()).isEqualTo("session-123");
        assertThat(response.getBody().isClarification()).isFalse();
        
        verify(wikiChatService).chat("session-123", projectExternalId, "How does auth work?");
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
        verify(wikiChatService).clearSession(sessionId);
    }
}
