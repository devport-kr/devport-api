package kr.devport.api.domain.wiki.infrastructure

import kr.devport.api.domain.wiki.entity.WikiChatMessage
import kr.devport.api.domain.wiki.entity.WikiChatSession
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/** Out-port: wiki chat session persistence (entity-backed; JPA adapter implements it). */
interface WikiChatSessionRepository {
    fun findByExternalId(externalId: String): Optional<WikiChatSession>

    fun findByUserIdOrderByLastMessageAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<WikiChatSession>

    fun findByUserIdAndProjectExternalIdOrderByLastMessageAtDesc(
        userId: Long,
        projectExternalId: String,
        pageable: Pageable,
    ): Page<WikiChatSession>

    fun findByUserIdAndSessionTypeOrderByLastMessageAtDesc(
        userId: Long,
        sessionType: WikiChatSessionType,
        pageable: Pageable,
    ): Page<WikiChatSession>

    fun save(session: WikiChatSession): WikiChatSession

    fun delete(session: WikiChatSession)

    fun deleteByExpiresAtBefore(now: LocalDateTime)
}

/** Out-port: wiki chat message persistence. */
interface WikiChatMessageRepository {
    fun findBySessionOrderByCreatedAtAsc(session: WikiChatSession): List<WikiChatMessage>

    fun findTop20BySessionOrderByCreatedAtDesc(session: WikiChatSession): List<WikiChatMessage>

    fun countBySession(session: WikiChatSession): Int

    fun countMapForSessions(sessions: List<WikiChatSession>): Map<Long, Long>

    fun save(message: WikiChatMessage): WikiChatMessage
}

/**
 * Out-port: wiki section chunk persistence, including pgvector/lexical similarity search. The JDBC
 * adapter implements the custom search methods; derived methods are JPA-backed.
 */
interface WikiSectionChunkRepository {
    fun findByProjectExternalId(projectExternalId: String): List<WikiSectionChunk>

    fun findAllSummaryChunks(): List<WikiSectionChunk>

    fun deleteByProjectExternalId(projectExternalId: String)

    fun findSimilarChunksWithScore(
        projectExternalId: String,
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow>

    fun findSimilarChunksGlobalWithScore(
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow>

    fun findLexicalCandidates(
        projectExternalId: String,
        question: String,
        limit: Int,
    ): List<ScoredChunkRow>
}

/** A wiki section chunk paired with its retrieval score (vector similarity or lexical). */
data class ScoredChunkRow(
    val chunk: WikiSectionChunk,
    val score: Double,
)
