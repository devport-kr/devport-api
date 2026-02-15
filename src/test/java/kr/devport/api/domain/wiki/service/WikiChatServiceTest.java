package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiChatServiceTest {

    @Mock
    private WikiRetrievalService retrievalService;

    @Mock
    private WikiChatSessionStore sessionStore;

    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private WikiChatService wikiChatService;

    @Test
    @DisplayName("clearSession delegates to session store")
    void clearSession_delegatesToStore() {
        // Given
        String sessionId = "session-123";

        // When
        wikiChatService.clearSession(sessionId);

        // Then
        verify(sessionStore).clearSession(sessionId);
    }

    @Test
    @DisplayName("hasActiveSession delegates to session store")
    void hasActiveSession_delegatesToStore() {
        // Given
        String sessionId = "session-123";
        when(sessionStore.hasActiveSession(sessionId)).thenReturn(true);

        // When
        boolean hasActive = wikiChatService.hasActiveSession(sessionId);

        // Then
        assertThat(hasActive).isTrue();
        verify(sessionStore).hasActiveSession(sessionId);
    }
}
