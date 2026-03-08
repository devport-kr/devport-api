package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiChatServiceTest {

    @Mock
    private WikiRetrievalService retrievalService;

    @Mock
    private WikiChatSessionStore sessionStore;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient openAIClient;

    @InjectMocks
    private WikiChatService wikiChatService;

    @BeforeEach
    void setUp() {
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)).choices().getFirst().message().content())
                .thenReturn(java.util.Optional.of("""
                        {"answer":"요약하면 인증 진입점은 SecurityConfig와 JwtAuthenticationFilter예요.","isClarification":false,"clarificationOptions":[],"suggestedNextQuestions":[],"usedPreviousContext":false}
                        """));
    }

    @Test
    @DisplayName("chat keeps only the most recent relevant turns in the model prompt")
    void chat_keepsOnlyRecentRelevantTurnsInPrompt() {
        when(retrievalService.retrieveContext("github:repo", "JWT 흐름 설명해줘"))
                .thenReturn(strongContext());
        when(sessionStore.loadRecentTurns("session-123", "github:repo"))
                .thenReturn(List.of(
                        turn("아주 예전 질문", "예전 답변", false),
                        turn("로그인 흐름이 어디서 시작돼?", "SecurityConfig에서 시작돼요.", false),
                        turn("그다음 JWT 검사는?", "JwtAuthenticationFilter가 담당해요.", false)
                ));
        when(sessionStore.hasActiveSession("session-123")).thenReturn(true);

        WikiChatResult result = wikiChatService.chat("session-123", "github:repo", "JWT 흐름 설명해줘");

        ArgumentCaptor<ChatCompletionCreateParams> captor = ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
        verify(openAIClient.chat().completions()).create(captor.capture());
        String messagesText = captor.getValue().messages().toString();

        assertThat(result.isClarification()).isFalse();
        assertThat(messagesText).contains("로그인 흐름이 어디서 시작돼?");
        assertThat(messagesText).contains("그다음 JWT 검사는?");
        assertThat(messagesText).doesNotContain("아주 예전 질문");
    }

    @Test
    @DisplayName("chat drops stale memory on topic shift and keeps weak-grounding replies short")
    void chat_dropsStaleMemoryOnTopicShiftAndKeepsWeakGroundingRepliesShort() {
        when(retrievalService.retrieveContext("github:repo", "배포 파이프라인이 어디에 있어?"))
                .thenReturn(new WikiRetrievalContext(
                        "github:repo",
                        "# Repository Context\n\n## CI\n.github/workflows/deploy.yml",
                        true,
                        true,
                        List.of(new WikiRetrievedChunk("ci", null, "CI", ".github/workflows/deploy.yml", 0.4d, 0.2d, ".github/workflows/deploy.yml")),
                        List.of("deploy.yml 기준으로 단계별로 설명해줘", "배포에 쓰는 secret 목록을 알려줘", "실패 시 롤백 흐름을 알려줘")
                ));
        when(sessionStore.loadRecentTurns("session-123", "github:repo"))
                .thenReturn(List.of(
                        turn("JWT 구조 알려줘", "인증 필터 중심이에요.", false),
                        turn("리프레시 토큰은?", "재발급 API에서 처리해요.", false)
                ));
        when(sessionStore.hasActiveSession("session-123")).thenReturn(true);
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)).choices().getFirst().message().content())
                .thenReturn(java.util.Optional.of("""
                        {"answer":"요약하면 배포 정보는 CI 설정 위주로만 확인돼요.","isClarification":false,"clarificationOptions":[],"suggestedNextQuestions":["deploy.yml 기준으로 단계별로 설명해줘","배포에 쓰는 secret 목록을 알려줘","실패 시 롤백 흐름을 알려줘"],"usedPreviousContext":true}
                        """));

        WikiChatResult result = wikiChatService.chat("session-123", "github:repo", "배포 파이프라인이 어디에 있어?");

        assertThat(result.isClarification()).isFalse();
        assertThat(result.usedPreviousContext()).isFalse();
        assertThat(result.suggestedNextQuestions()).hasSize(3);
        assertThat(result.answer()).startsWith("요약하면");
    }

    @Test
    @DisplayName("clearSession delegates to session store")
    void clearSession_delegatesToStore() {
        wikiChatService.clearSession("session-123");

        verify(sessionStore).clearSession("session-123");
    }

    @Test
    @DisplayName("hasActiveSession delegates to session store")
    void hasActiveSession_delegatesToStore() {
        when(sessionStore.hasActiveSession("session-123")).thenReturn(true);

        boolean hasActive = wikiChatService.hasActiveSession("session-123");

        assertThat(hasActive).isTrue();
        verify(sessionStore).hasActiveSession("session-123");
    }

    private WikiRetrievalContext strongContext() {
        return new WikiRetrievalContext(
                "github:repo",
                "# Repository Context\n\n## 인증 흐름\nSecurityConfig -> JwtAuthenticationFilter",
                true,
                false,
                List.of(new WikiRetrievedChunk(
                        "architecture",
                        "auth",
                        "인증 흐름",
                        "SecurityConfig -> JwtAuthenticationFilter",
                        0.9d,
                        0.7d,
                        "src/main/java/kr/devport/api/domain/common/config/SecurityConfig.java"
                )),
                List.of()
        );
    }

    private ChatTurn turn(String question, String answer, boolean clarification) {
        return ChatTurn.builder()
                .question(question)
                .answer(answer)
                .wasClarification(clarification)
                .build();
    }
}
