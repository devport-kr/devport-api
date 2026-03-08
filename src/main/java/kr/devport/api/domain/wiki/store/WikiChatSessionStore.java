package kr.devport.api.domain.wiki.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-backed session store for wiki chat turns.
 * Sessions expire via Redis TTL and remain available across restarts and nodes.
 * Keeps storage bounded by storing only recent turns per session.
 */
@Component
@RequiredArgsConstructor
public class WikiChatSessionStore {

    private static final int DEFAULT_TTL_MINUTES = 30;
    private static final int MAX_TURNS_PER_SESSION = 10;
    private static final int MAX_RECENT_TURNS = 3;
    private static final Duration SESSION_TTL = Duration.ofMinutes(DEFAULT_TTL_MINUTES);
    private static final String KEY_PREFIX = "wiki:session:";

    private final RedisTemplate<String, Object> redisTemplate;

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
        String key = KEY_PREFIX + sessionId;
        ChatSession session = fetchSession(key);
        if (session == null || !projectMatches(session, projectExternalId)) {
            session = newSession(sessionId, projectExternalId);
        }

        ChatTurn turn = ChatTurn.builder()
                .question(question)
                .answer(answer)
                .timestamp(Instant.now())
                .wasClarification(wasClarification)
                .build();

        session.getTurns().add(turn);

        if (session.getTurns().size() > MAX_TURNS_PER_SESSION) {
            session.getTurns().remove(0);
        }

        session.setExpiresAt(calculateExpiration());
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);
    }

    /**
     * Load chat turns for session.
     * Returns empty list if session not found or expired.
     *
     * @param sessionId Session identifier
     * @return List of chat turns
     */
    public List<ChatTurn> loadTurns(String sessionId) {
        return loadTurnsInternal(sessionId, null);
    }

    public List<ChatTurn> loadTurns(String sessionId, String projectExternalId) {
        return loadTurnsInternal(sessionId, projectExternalId);
    }

    public List<ChatTurn> loadRecentTurns(String sessionId, String projectExternalId) {
        List<ChatTurn> turns = loadTurnsInternal(sessionId, projectExternalId);
        if (turns.size() <= MAX_RECENT_TURNS) {
            return turns;
        }
        return new ArrayList<>(turns.subList(turns.size() - MAX_RECENT_TURNS, turns.size()));
    }

    Optional<ChatSession> getSession(String sessionId) {
        return Optional.ofNullable(fetchSession(KEY_PREFIX + sessionId));
    }

    /**
     * Check if session exists and is not expired.
     *
     * @param sessionId Session identifier
     * @return true if session is active
     */
    public boolean hasActiveSession(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + sessionId));
    }

    /**
     * Clear specific session.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    private List<ChatTurn> loadTurnsInternal(String sessionId, String projectExternalId) {
        Optional<ChatSession> session = getSession(sessionId);
        if (session.isEmpty()) {
            return List.of();
        }

        if (projectExternalId != null && !projectMatches(session.get(), projectExternalId)) {
            return List.of();
        }

        return new ArrayList<>(session.get().getTurns());
    }

    private ChatSession newSession(String sessionId, String projectExternalId) {
        return ChatSession.builder()
                .sessionId(sessionId)
                .projectExternalId(projectExternalId)
                .turns(new ArrayList<>())
                .expiresAt(calculateExpiration())
                .build();
    }

    private boolean projectMatches(ChatSession session, String projectExternalId) {
        return projectExternalId != null && projectExternalId.equals(session.getProjectExternalId());
    }

    /**
     * Load a session from Redis, returning null if missing or deserialized to an unexpected type.
     */
    private ChatSession fetchSession(String key) {
        Object raw = redisTemplate.opsForValue().get(key);
        return raw instanceof ChatSession session ? session : null;
    }

    /**
     * Calculate expiration time from now + TTL.
     */
    private Instant calculateExpiration() {
        return Instant.now().plus(DEFAULT_TTL_MINUTES, ChronoUnit.MINUTES);
    }

    /**
     * Get active session count for observability.
     * Uses Redis KEYS and should not be treated as a hot-path operation.
     */
    public int getActiveSessionCount() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }
}
