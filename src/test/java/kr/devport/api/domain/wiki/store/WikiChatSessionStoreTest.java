package kr.devport.api.domain.wiki.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikiChatSessionStoreTest {

    private final WikiChatSessionStore sessionStore = new WikiChatSessionStore();

    @Test
    @DisplayName("saveTurn keeps recent turns available for the active session")
    void saveTurn_keepsRecentTurnsAvailableForActiveSession() {
        sessionStore.saveTurn("session-1", "github:repo", "auth?", "JWT를 사용해요.", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("session-1", "github:repo");

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

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("session-2", "github:repo");

        assertThat(turns).hasSize(10);
        assertThat(turns.getFirst().getQuestion()).isEqualTo("q2");
        assertThat(turns.getLast().getQuestion()).isEqualTo("q11");
    }

    @Test
    @DisplayName("reusing a session ID on another project resets stale memory")
    void saveTurn_resetsStaleMemoryWhenProjectChanges() {
        sessionStore.saveTurn("shared-session", "github:repo-a", "A 질문", "A 답변", false);
        sessionStore.saveTurn("shared-session", "github:repo-b", "B 질문", "B 답변", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadTurns("shared-session", "github:repo-b");

        assertThat(turns)
                .extracting(WikiChatSessionStore.ChatTurn::getQuestion)
                .containsExactly("B 질문");
        assertThat(sessionStore.loadTurns("shared-session", "github:repo-a")).isEmpty();
    }

    @Test
    @DisplayName("recent-turn selection returns only the last three turns for prompt assembly")
    void loadRecentTurns_returnsOnlyLastThreeTurns() {
        sessionStore.saveTurn("session-3", "github:repo", "빌드 질문", "빌드 답변", false);
        sessionStore.saveTurn("session-3", "github:repo", "배포 질문", "배포 답변", false);
        sessionStore.saveTurn("session-3", "github:repo", "테스트 질문", "테스트 답변", false);
        sessionStore.saveTurn("session-3", "github:repo", "아키텍처 질문", "아키텍처 답변", false);

        List<WikiChatSessionStore.ChatTurn> turns = sessionStore.loadRecentTurns("session-3", "github:repo");

        assertThat(turns)
                .extracting(WikiChatSessionStore.ChatTurn::getQuestion)
                .containsExactly("배포 질문", "테스트 질문", "아키텍처 질문");
    }

    @Test
    @DisplayName("saveTurn refreshes ttl and clearSession removes the session immediately")
    void saveTurn_refreshesTtlAndClearSessionRemovesSession() {
        sessionStore.saveTurn("session-4", "github:repo", "첫 질문", "첫 답변", false);
        Instant initialExpiry = sessionStore.getSession("session-4").orElseThrow().getExpiresAt();

        sessionStore.saveTurn("session-4", "github:repo", "두 번째 질문", "두 번째 답변", true);

        Instant refreshedExpiry = sessionStore.getSession("session-4").orElseThrow().getExpiresAt();
        assertThat(refreshedExpiry).isAfter(initialExpiry);

        sessionStore.clearSession("session-4");

        assertThat(sessionStore.hasActiveSession("session-4")).isFalse();
        assertThat(sessionStore.loadTurns("session-4", "github:repo")).isEmpty();
    }
}
