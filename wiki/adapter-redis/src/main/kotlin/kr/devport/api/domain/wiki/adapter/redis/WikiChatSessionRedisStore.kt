package kr.devport.api.domain.wiki.adapter.redis

import kr.devport.api.domain.wiki.infrastructure.ChatSession
import kr.devport.api.domain.wiki.infrastructure.ChatTurn
import kr.devport.api.domain.wiki.infrastructure.WikiChatSessionStore
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Redis adapter for [WikiChatSessionStore]. Sessions expire via Redis TTL and survive restarts and
 * node changes. Turns are stored as plain maps (not the entity type) so the format is stable across
 * deployments. The [KEY_PREFIX] and map serialization are preserved so live sessions survive a deploy.
 */
@Component
class WikiChatSessionRedisStore(
    private val redisTemplate: RedisTemplate<String, Any>,
) : WikiChatSessionStore {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Save a chat turn with TTL refresh. Creates a new session if missing, prunes old turns at capacity.
     */
    override fun saveTurn(
        sessionId: String,
        projectExternalId: String?,
        question: String,
        answer: String,
        wasClarification: Boolean,
    ) {
        val key = KEY_PREFIX + sessionId
        var session = fetchSession(key)
        if (session == null || !projectMatches(session, projectExternalId)) {
            session = newSession(sessionId, projectExternalId)
        }

        val turn =
            ChatTurn(
                question = question,
                answer = answer,
                timestamp = Instant.now(),
                wasClarification = wasClarification,
            )
        session.turns.add(turn)

        if (session.turns.size > MAX_TURNS_PER_SESSION) {
            session.turns.removeAt(0)
        }

        session.expiresAt = calculateExpiration()
        redisTemplate.opsForValue().set(key, serializeSession(session), SESSION_TTL)
    }

    /** Load chat turns for a session. Empty if missing or expired. */
    override fun loadTurns(sessionId: String): List<ChatTurn> = loadTurnsInternal(sessionId, null)

    override fun loadTurns(
        sessionId: String,
        projectExternalId: String?,
    ): List<ChatTurn> = loadTurnsInternal(sessionId, projectExternalId)

    override fun loadRecentTurns(
        sessionId: String,
        projectExternalId: String?,
    ): List<ChatTurn> {
        val turns = loadTurnsInternal(sessionId, projectExternalId)
        return if (turns.size <= MAX_RECENT_TURNS) {
            turns
        } else {
            ArrayList(turns.subList(turns.size - MAX_RECENT_TURNS, turns.size))
        }
    }

    override fun getSession(sessionId: String): ChatSession? = fetchSession(KEY_PREFIX + sessionId)

    /** True if the session exists and is not expired. */
    override fun hasActiveSession(sessionId: String): Boolean = redisTemplate.hasKey(KEY_PREFIX + sessionId) == true

    /** Clear a specific session. */
    override fun clearSession(sessionId: String) {
        redisTemplate.delete(KEY_PREFIX + sessionId)
    }

    private fun loadTurnsInternal(
        sessionId: String,
        projectExternalId: String?,
    ): List<ChatTurn> {
        val session = getSession(sessionId) ?: return emptyList()
        if (projectExternalId != null && !projectMatches(session, projectExternalId)) {
            return emptyList()
        }
        return ArrayList(session.turns)
    }

    private fun newSession(
        sessionId: String,
        projectExternalId: String?,
    ): ChatSession =
        ChatSession(
            sessionId = sessionId,
            projectExternalId = projectExternalId,
            turns = mutableListOf(),
            expiresAt = calculateExpiration(),
        )

    private fun projectMatches(
        session: ChatSession,
        projectExternalId: String?,
    ): Boolean = projectExternalId != null && projectExternalId == session.projectExternalId

    /**
     * Load a session from Redis, returning null if missing or deserialized to an unexpected type.
     * SerializationException is treated as a cache miss (stale data from a prior deployment).
     */
    private fun fetchSession(key: String): ChatSession? {
        val raw: Any? =
            try {
                redisTemplate.opsForValue().get(key)
            } catch (ex: SerializationException) {
                log.warn("wiki-session: deserialization failed for key={}, treating as cache miss: {}", key, ex.message)
                return null
            }
        if (raw is ChatSession) {
            return raw
        }
        if (raw !is Map<*, *>) {
            return null
        }
        return deserializeSession(raw)
    }

    private fun calculateExpiration(): Instant = Instant.now().plus(DEFAULT_TTL_MINUTES.toLong(), ChronoUnit.MINUTES)

    private fun serializeSession(session: ChatSession): Map<String, Any?> {
        val serializedTurns = session.turns.mapTo(ArrayList()) { serializeTurn(it) }
        return hashMapOf(
            SESSION_ID_KEY to session.sessionId,
            PROJECT_EXTERNAL_ID_KEY to session.projectExternalId,
            EXPIRES_AT_KEY to session.expiresAt.toString(),
            TURNS_KEY to serializedTurns,
        )
    }

    private fun serializeTurn(turn: ChatTurn): Map<String, Any?> =
        hashMapOf(
            QUESTION_KEY to turn.question,
            ANSWER_KEY to turn.answer,
            TIMESTAMP_KEY to turn.timestamp.toString(),
            WAS_CLARIFICATION_KEY to turn.wasClarification,
        )

    private fun deserializeSession(payload: Map<*, *>): ChatSession? {
        val sessionIdValue = payload[SESSION_ID_KEY]
        val expiresAtValue = payload[EXPIRES_AT_KEY]
        val turnsValue = payload[TURNS_KEY]
        val projectExternalIdValue = payload[PROJECT_EXTERNAL_ID_KEY]

        if (sessionIdValue !is String || expiresAtValue !is String) {
            return null
        }

        val turns = deserializeTurns(turnsValue) ?: return null

        return try {
            ChatSession(
                sessionId = sessionIdValue,
                projectExternalId = projectExternalIdValue as? String,
                expiresAt = Instant.parse(expiresAtValue),
                turns = turns,
            )
        } catch (ignored: Exception) {
            null
        }
    }

    private fun deserializeTurns(turnsValue: Any?): MutableList<ChatTurn>? {
        if (turnsValue !is List<*>) {
            return null
        }
        val turns = mutableListOf<ChatTurn>()
        for (turnRaw in turnsValue) {
            if (turnRaw !is Map<*, *>) {
                return null
            }
            val turn = deserializeTurn(turnRaw) ?: return null
            turns.add(turn)
        }
        return turns
    }

    private fun deserializeTurn(payload: Map<*, *>): ChatTurn? {
        val questionValue = payload[QUESTION_KEY]
        val answerValue = payload[ANSWER_KEY]
        val timestampValue = payload[TIMESTAMP_KEY]
        val wasClarificationValue = payload[WAS_CLARIFICATION_KEY]

        if (questionValue !is String ||
            answerValue !is String ||
            timestampValue !is String ||
            wasClarificationValue !is Boolean
        ) {
            return null
        }

        return try {
            ChatTurn(
                question = questionValue,
                answer = answerValue,
                timestamp = Instant.parse(timestampValue),
                wasClarification = wasClarificationValue,
            )
        } catch (ignored: Exception) {
            null
        }
    }

    /** Active session count for observability. Uses Redis KEYS; not a hot-path operation. */
    override fun getActiveSessionCount(): Int = redisTemplate.keys("$KEY_PREFIX*")?.size ?: 0

    companion object {
        private const val DEFAULT_TTL_MINUTES = 30
        private const val MAX_TURNS_PER_SESSION = 10
        private const val MAX_RECENT_TURNS = 3
        private val SESSION_TTL: Duration = Duration.ofMinutes(DEFAULT_TTL_MINUTES.toLong())
        private const val KEY_PREFIX = "wiki:session:"
        private const val SESSION_ID_KEY = "sessionId"
        private const val TURNS_KEY = "turns"
        private const val EXPIRES_AT_KEY = "expiresAt"
        private const val PROJECT_EXTERNAL_ID_KEY = "projectExternalId"
        private const val QUESTION_KEY = "question"
        private const val ANSWER_KEY = "answer"
        private const val TIMESTAMP_KEY = "timestamp"
        private const val WAS_CLARIFICATION_KEY = "wasClarification"
    }
}
