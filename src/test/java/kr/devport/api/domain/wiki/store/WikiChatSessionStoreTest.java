package kr.devport.api.domain.wiki.store;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikiChatSessionStoreTest {

    private final WikiChatSessionStore sessionStore = new WikiChatSessionStore();

    @Test
    @DisplayName("saveTurn keeps recent turns available for the active session")
    void saveTurn_keepsRecentTurnsAvailableForActiveSession() {
        sessionStore.saveTurn("session-1", "github:repo", "auth?", "JWT를 사용해요.", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("session-1");

        assertThat(turns).hasSize(1);
        assertThat(turns.getFirst().getQuestion()).isEqualTo("auth?");
        assertThat(turns.getFirst().getAnswer()).isEqualTo("JWT를 사용해요.");
        assertThat(sessionStore.hasActiveSession("session-1")).isTrue();
    }

    @Test
    @DisplayName("saveTurn prunes oldest turns after the session reaches capacity")
    void saveTurn_prunesOldestTurnsAfterSessionReachesCapacity() {
        for (int index = 1; index <= 11; index++) {
            sessionStore.saveTurn(
                    "session-2",
                    "github:repo",
                    "q" + index,
                    "a" + index,
                    false
            );
        }

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("session-2");

        assertThat(turns).hasSize(10);
        assertThat(turns.getFirst().getQuestion()).isEqualTo("q2");
        assertThat(turns.getLast().getQuestion()).isEqualTo("q11");
    }

    @Disabled("Enable in 06-02 when project-mismatch reset behavior is implemented")
    @Test
    @DisplayName("reusing a session ID on another project resets stale memory")
    void saveTurn_resetsStaleMemoryWhenProjectChanges() {
        sessionStore.saveTurn("shared-session", "github:repo-a", "A 질문", "A 답변", false);
        sessionStore.saveTurn("shared-session", "github:repo-b", "B 질문", "B 답변", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("shared-session");

        assertThat(turns)
                .extracting(WikiChatSessionStore.ChatTurn::getQuestion)
                .containsExactly("B 질문");
    }

    @Disabled("Enable in 06-02 when topic-shift selection helpers are added")
    @Test
    @DisplayName("recent-turn selection keeps the last relevant turns after a topic shift")
    void selectRecentTurns_keepsOnlyRelevantTurnsAfterTopicShift() {
        sessionStore.saveTurn("session-3", "github:repo", "빌드 질문", "빌드 답변", false);
        sessionStore.saveTurn("session-3", "github:repo", "배포 질문", "배포 답변", false);
        sessionStore.saveTurn("session-3", "github:repo", "테스트 질문", "테스트 답변", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("session-3");

        assertThat(turns)
                .extracting(WikiChatSessionStore.ChatTurn::getQuestion)
                .containsExactly("배포 질문", "테스트 질문");
    }
}
