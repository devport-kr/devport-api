package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.wiki.dto.response.WikiMessageResponse
import kr.devport.api.domain.wiki.dto.response.WikiSessionListResponse
import kr.devport.api.domain.wiki.dto.response.WikiSessionResponse
import kr.devport.api.domain.wiki.entity.WikiChatMessage
import kr.devport.api.domain.wiki.entity.WikiChatSession
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import kr.devport.api.domain.wiki.exception.WikiSessionNotFoundException
import kr.devport.api.domain.wiki.repository.WikiChatMessageRepository
import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * All DB persistence for wiki chat sessions and messages.
 */
@Service
class WikiChatSessionPersistenceService(
    private val sessionRepository: WikiChatSessionRepository,
    private val messageRepository: WikiChatMessageRepository,
    private val userRepository: UserRepository,
) {
    /** Find or create a session. Refreshes last_message_at and expiresAt on every call. */
    @Transactional
    fun findOrCreateSession(
        externalId: String,
        user: User?,
        projectExternalId: String?,
        sessionType: WikiChatSessionType,
    ): WikiChatSession =
        sessionRepository
            .findByExternalId(externalId)
            .map { session ->
                val now = LocalDateTime.now()
                session.lastMessageAt = now
                session.expiresAt = now.plusDays(SESSION_TTL_DAYS)
                sessionRepository.save(session)
            }.orElseGet {
                val now = LocalDateTime.now()
                val session =
                    WikiChatSession().apply {
                        this.externalId = externalId
                        this.user = user
                        this.projectExternalId = projectExternalId
                        this.sessionType = sessionType
                        this.lastMessageAt = now
                        this.expiresAt = now.plusDays(SESSION_TTL_DAYS)
                    }
                sessionRepository.save(session)
            }

    /** Save a user message to the DB. */
    @Transactional
    fun saveUserMessage(
        session: WikiChatSession,
        question: String,
    ): WikiChatMessage {
        val message =
            WikiChatMessage().apply {
                this.session = session
                role = "USER"
                content = question
                isClarification = false
            }
        return messageRepository.save(message)
    }

    /** Save an assistant message to the DB. */
    @Transactional
    fun saveAssistantMessage(
        session: WikiChatSession,
        answer: String,
        isClarification: Boolean,
    ): WikiChatMessage {
        val message =
            WikiChatMessage().apply {
                this.session = session
                role = "ASSISTANT"
                content = answer
                this.isClarification = isClarification
            }
        return messageRepository.save(message)
    }

    /**
     * Load recent messages from DB as ChatTurn list (oldest-first). Fallback when the Redis session
     * is cold (resumed session).
     */
    @Transactional(readOnly = true)
    fun loadRecentMessages(
        externalId: String,
        limit: Int,
    ): List<ChatTurn> =
        sessionRepository
            .findByExternalId(externalId)
            .map { session ->
                val messages = messageRepository.findTop20BySessionOrderByCreatedAtDesc(session).toMutableList()
                // Reverse to chronological order
                messages.reverse()
                // Pair USER/ASSISTANT messages into turns
                val turns = mutableListOf<ChatTurn>()
                var i = 0
                while (i < messages.size - 1) {
                    val msg = messages[i]
                    val next = messages[i + 1]
                    if (msg.role == "USER" && next.role == "ASSISTANT") {
                        turns.add(
                            ChatTurn(
                                question = msg.content,
                                answer = next.content,
                                wasClarification = next.isClarification,
                            ),
                        )
                        i++ // skip the assistant message
                    }
                    i++
                }
                val recent = if (turns.size > limit) turns.subList(turns.size - limit, turns.size) else turns
                recent.toList()
            }.orElse(emptyList())

    /** Load all messages for a session (auth-checked). */
    @Transactional(readOnly = true)
    fun loadSessionMessages(
        externalId: String,
        userId: Long,
    ): List<WikiMessageResponse> {
        val session =
            sessionRepository
                .findByExternalId(externalId)
                .orElseThrow { WikiSessionNotFoundException("세션을 찾을 수 없습니다.") }

        if (session.user == null || session.user?.id != userId) {
            throw WikiSessionNotFoundException("세션을 찾을 수 없습니다.")
        }

        return messageRepository.findBySessionOrderByCreatedAtAsc(session).map { msg ->
            WikiMessageResponse(
                role = msg.role,
                content = msg.content,
                isClarification = msg.isClarification,
                createdAt = msg.createdAt,
            )
        }
    }

    /** All sessions for a user (paginated). */
    @Transactional(readOnly = true)
    fun getUserSessions(
        userId: Long,
        page: Int,
        size: Int,
    ): WikiSessionListResponse {
        val user = resolveUser(userId)
        val sessionPage = sessionRepository.findByUserOrderByLastMessageAtDesc(user, PageRequest.of(page, size))
        return toListResponse(sessionPage)
    }

    /** Project sessions for a user (paginated). */
    @Transactional(readOnly = true)
    fun getProjectSessions(
        userId: Long,
        projectExternalId: String,
        page: Int,
        size: Int,
    ): WikiSessionListResponse {
        val user = resolveUser(userId)
        val sessionPage =
            sessionRepository.findByUserAndProjectExternalIdOrderByLastMessageAtDesc(
                user,
                projectExternalId,
                PageRequest.of(page, size),
            )
        return toListResponse(sessionPage)
    }

    /** Global chat sessions for a user (paginated). */
    @Transactional(readOnly = true)
    fun getGlobalSessions(
        userId: Long,
        page: Int,
        size: Int,
    ): WikiSessionListResponse {
        val user = resolveUser(userId)
        val sessionPage =
            sessionRepository.findByUserAndSessionTypeOrderByLastMessageAtDesc(
                user,
                WikiChatSessionType.GLOBAL,
                PageRequest.of(page, size),
            )
        return toListResponse(sessionPage)
    }

    private fun resolveUser(userId: Long): User = userRepository.findById(userId).orElseThrow { IllegalStateException("User not found") }

    /** Hard delete a session (auth-checked). Cascade deletes messages automatically. */
    @Transactional
    fun deleteSession(
        externalId: String,
        userId: Long,
    ) {
        val session =
            sessionRepository
                .findByExternalId(externalId)
                .orElseThrow { WikiSessionNotFoundException("세션을 찾을 수 없습니다.") }

        if (session.user == null || session.user?.id != userId) {
            throw WikiSessionNotFoundException("세션을 찾을 수 없습니다.")
        }

        sessionRepository.delete(session)
    }

    /** True if the session has 0 prior messages (first-message check). */
    @Transactional(readOnly = true)
    fun isFirstMessage(externalId: String): Boolean =
        sessionRepository
            .findByExternalId(externalId)
            .map { messageRepository.countBySession(it) == 0 }
            .orElse(true)

    private fun toListResponse(sessionPage: Page<WikiChatSession>): WikiSessionListResponse {
        val sessions = sessionPage.content
        val countMap = if (sessions.isEmpty()) emptyMap() else messageRepository.countMapForSessions(sessions)

        val responses =
            sessions.map { session ->
                WikiSessionResponse(
                    sessionId = session.externalId,
                    title = session.title,
                    sessionType = session.sessionType?.name,
                    projectExternalId = session.projectExternalId,
                    createdAt = session.createdAt,
                    lastMessageAt = session.lastMessageAt,
                    messageCount = (session.id?.let { countMap[it] } ?: 0L).toInt(),
                )
            }

        return WikiSessionListResponse(
            sessions = responses,
            totalPages = sessionPage.totalPages,
            totalElements = sessionPage.totalElements,
            page = sessionPage.number,
            size = sessionPage.size,
        )
    }

    companion object {
        private const val SESSION_TTL_DAYS = 10L
    }
}
