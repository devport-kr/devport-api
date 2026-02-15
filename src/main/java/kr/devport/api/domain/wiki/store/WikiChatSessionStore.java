package kr.devport.api.domain.wiki.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Short-lived session memory store for wiki chat turns.
 * Sessions expire after TTL with no cross-session persistence.
 * Keeps memory footprint minimal by storing recent turns only.
 */
@Component
public class WikiChatSessionStore {

    private static final int DEFAULT_TTL_MINUTES = 30;
    private static final int MAX_TURNS_PER_SESSION = 10;

    private final ConcurrentMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    /**
     * Chat session with TTL expiration tracking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSession {
        private String sessionId;
        private List<ChatTurn> turns;
        private Instant expiresAt;
        private String projectExternalId;
    }

    /**
     * Single chat turn (question + answer pair).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatTurn {
        private String question;
        private String answer;
        private Instant timestamp;
        private boolean wasClarification;
    }

    /**
     * Save chat turn to session with TTL refresh.
     * Creates new session if not exists, prunes old turns if at capacity.
     *
     * @param sessionId Session identifier
     * @param projectExternalId Project context
     * @param question User question
     * @param answer Generated answer
     * @param wasClarification Whether this was a clarification response
     */
    public void saveTurn(
            String sessionId,
            String projectExternalId,
            String question,
            String answer,
            boolean wasClarification
    ) {
        // Remove expired sessions first
        cleanExpiredSessions();

        ChatSession session = sessions.computeIfAbsent(sessionId, id ->
                ChatSession.builder()
                        .sessionId(id)
                        .projectExternalId(projectExternalId)
                        .turns(new ArrayList<>())
                        .expiresAt(calculateExpiration())
                        .build()
        );

        // Add new turn
        ChatTurn turn = ChatTurn.builder()
                .question(question)
                .answer(answer)
                .timestamp(Instant.now())
                .wasClarification(wasClarification)
                .build();

        session.getTurns().add(turn);

        // Prune old turns if at capacity (keep recent turns only)
        if (session.getTurns().size() > MAX_TURNS_PER_SESSION) {
            session.getTurns().remove(0); // Remove oldest
        }

        // Refresh TTL on activity
        session.setExpiresAt(calculateExpiration());
    }

    /**
     * Load chat turns for session.
     * Returns empty list if session not found or expired.
     *
     * @param sessionId Session identifier
     * @return List of chat turns
     */
    public List<ChatTurn> loadTurns(String sessionId) {
        cleanExpiredSessions();

        ChatSession session = sessions.get(sessionId);
        if (session == null || isExpired(session)) {
            return List.of();
        }

        return new ArrayList<>(session.getTurns());
    }

    /**
     * Check if session exists and is not expired.
     *
     * @param sessionId Session identifier
     * @return true if session is active
     */
    public boolean hasActiveSession(String sessionId) {
        cleanExpiredSessions();

        ChatSession session = sessions.get(sessionId);
        return session != null && !isExpired(session);
    }

    /**
     * Clear specific session.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Clean all expired sessions (automatic garbage collection).
     */
    private void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    /**
     * Check if session is expired.
     */
    private boolean isExpired(ChatSession session) {
        return Instant.now().isAfter(session.getExpiresAt());
    }

    /**
     * Calculate expiration time from now + TTL.
     */
    private Instant calculateExpiration() {
        return Instant.now().plus(DEFAULT_TTL_MINUTES, ChronoUnit.MINUTES);
    }

    /**
     * Get active session count (for monitoring).
     */
    public int getActiveSessionCount() {
        cleanExpiredSessions();
        return sessions.size();
    }
}
