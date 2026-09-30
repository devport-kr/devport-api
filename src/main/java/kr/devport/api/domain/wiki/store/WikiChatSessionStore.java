package kr.devport.api.domain.wiki.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed session store for wiki chat turns.
 * Sessions expire via Redis TTL and remain available across restarts and nodes.
 * Keeps storage bounded by storing only recent turns per session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiChatSessionStore {

    private static final int DEFAULT_TTL_MINUTES = 30;
    private static final int MAX_TURNS_PER_SESSION = 10;
    private static final int MAX_RECENT_TURNS = 3;
    private static final Duration SESSION_TTL = Duration.ofMinutes(DEFAULT_TTL_MINUTES);
    private static final String KEY_PREFIX = "wiki:session:";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String TURNS_KEY = "turns";
    private static final String EXPIRES_AT_KEY = "expiresAt";
    private static final String PROJECT_EXTERNAL_ID_KEY = "projectExternalId";
    private static final String QUESTION_KEY = "question";
    private static final String ANSWER_KEY = "answer";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String WAS_CLARIFICATION_KEY = "wasClarification";

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
        redisTemplate.opsForValue().set(key, serializeSession(session), SESSION_TTL);
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
     * SerializationException is treated as a cache miss (stale data from a prior deployment).
     */
    private ChatSession fetchSession(String key) {
        Object raw;
        try {
            raw = redisTemplate.opsForValue().get(key);
        } catch (SerializationException ex) {
            log.warn("wiki-session: deserialization failed for key={}, treating as cache miss: {}", key, ex.getMessage());
            return null;
        }
        if (raw instanceof ChatSession session) {
            return session;
        }
        if (!(raw instanceof Map<?, ?> payload)) {
            return null;
        }
        return deserializeSession(payload);
    }

    /**
     * Calculate expiration time from now + TTL.
     */
    private Instant calculateExpiration() {
        return Instant.now().plus(DEFAULT_TTL_MINUTES, ChronoUnit.MINUTES);
    }

    private Map<String, Object> serializeSession(ChatSession session) {
        List<Map<String, Object>> serializedTurns = session.getTurns().stream()
                .map(this::serializeTurn)
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> payload = new HashMap<>();
        payload.put(SESSION_ID_KEY, session.getSessionId());
        payload.put(PROJECT_EXTERNAL_ID_KEY, session.getProjectExternalId());
        payload.put(EXPIRES_AT_KEY, session.getExpiresAt().toString());
        payload.put(TURNS_KEY, serializedTurns);
        return payload;
    }

    private Map<String, Object> serializeTurn(ChatTurn turn) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(QUESTION_KEY, turn.getQuestion());
        payload.put(ANSWER_KEY, turn.getAnswer());
        payload.put(TIMESTAMP_KEY, turn.getTimestamp().toString());
        payload.put(WAS_CLARIFICATION_KEY, turn.isWasClarification());
        return payload;
    }

    private ChatSession deserializeSession(Map<?, ?> payload) {
        Object sessionIdValue = payload.get(SESSION_ID_KEY);
        Object expiresAtValue = payload.get(EXPIRES_AT_KEY);
        Object turnsValue = payload.get(TURNS_KEY);
        Object projectExternalIdValue = payload.get(PROJECT_EXTERNAL_ID_KEY);

        if (!(sessionIdValue instanceof String sessionId) || !(expiresAtValue instanceof String expiresAtRaw)) {
            return null;
        }

        List<ChatTurn> turns = deserializeTurns(turnsValue);
        if (turns == null) {
            return null;
        }

        try {
            return ChatSession.builder()
                    .sessionId(sessionId)
                    .projectExternalId(projectExternalIdValue instanceof String projectExternalId ? projectExternalId : null)
                    .expiresAt(Instant.parse(expiresAtRaw))
                    .turns(turns)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<ChatTurn> deserializeTurns(Object turnsValue) {
        if (!(turnsValue instanceof List<?> turnsRaw)) {
            return null;
        }

        List<ChatTurn> turns = new ArrayList<>();
        for (Object turnRaw : turnsRaw) {
            if (!(turnRaw instanceof Map<?, ?> turnPayload)) {
                return null;
            }
            ChatTurn turn = deserializeTurn(turnPayload);
            if (turn == null) {
                return null;
            }
            turns.add(turn);
        }
        return turns;
    }

    private ChatTurn deserializeTurn(Map<?, ?> payload) {
        Object questionValue = payload.get(QUESTION_KEY);
        Object answerValue = payload.get(ANSWER_KEY);
        Object timestampValue = payload.get(TIMESTAMP_KEY);
        Object wasClarificationValue = payload.get(WAS_CLARIFICATION_KEY);

        if (!(questionValue instanceof String question)
                || !(answerValue instanceof String answer)
                || !(timestampValue instanceof String timestampRaw)
                || !(wasClarificationValue instanceof Boolean wasClarification)) {
            return null;
        }

        try {
            return ChatTurn.builder()
                    .question(question)
                    .answer(answer)
                    .timestamp(Instant.parse(timestampRaw))
                    .wasClarification(wasClarification)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
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
