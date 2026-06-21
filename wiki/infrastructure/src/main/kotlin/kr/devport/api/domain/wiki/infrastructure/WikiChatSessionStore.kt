package kr.devport.api.domain.wiki.infrastructure

import java.time.Instant

/**
 * Out-port: short-lived, TTL-bound store for wiki chat turns. The Redis adapter in
 * :wiki:adapter-redis implements it; the core only sees this contract. Turns are stored as plain
 * data (not the JPA entity) so the persisted format is stable across deployments.
 */
interface WikiChatSessionStore {
    fun saveTurn(
        sessionId: String,
        projectExternalId: String?,
        question: String,
        answer: String,
        wasClarification: Boolean,
    )

    fun loadTurns(sessionId: String): List<ChatTurn>

    fun loadTurns(
        sessionId: String,
        projectExternalId: String?,
    ): List<ChatTurn>

    fun loadRecentTurns(
        sessionId: String,
        projectExternalId: String?,
    ): List<ChatTurn>

    fun getSession(sessionId: String): ChatSession?

    fun hasActiveSession(sessionId: String): Boolean

    fun clearSession(sessionId: String)

    fun getActiveSessionCount(): Int
}

/** Chat session with TTL expiration tracking. */
data class ChatSession(
    var sessionId: String? = null,
    var turns: MutableList<ChatTurn> = mutableListOf(),
    var expiresAt: Instant? = null,
    var projectExternalId: String? = null,
)

/** Single chat turn (question + answer pair). */
data class ChatTurn(
    var question: String? = null,
    var answer: String? = null,
    var timestamp: Instant? = null,
    var wasClarification: Boolean = false,
)
