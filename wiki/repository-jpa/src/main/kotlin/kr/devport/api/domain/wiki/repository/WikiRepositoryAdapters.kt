package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.wiki.entity.WikiChatMessage
import kr.devport.api.domain.wiki.entity.WikiChatSession
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import kr.devport.api.domain.wiki.infrastructure.ScoredChunkRow
import kr.devport.api.domain.wiki.infrastructure.WikiChatMessageRepository
import kr.devport.api.domain.wiki.infrastructure.WikiChatSessionRepository
import kr.devport.api.domain.wiki.infrastructure.WikiSectionChunkRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
class WikiChatSessionRepositoryAdapter(
    private val jpa: WikiChatSessionJpaRepository,
) : WikiChatSessionRepository {
    override fun findByExternalId(externalId: String): Optional<WikiChatSession> = jpa.findByExternalId(externalId)

    override fun findByUserIdOrderByLastMessageAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<WikiChatSession> = jpa.findByUserIdOrderByLastMessageAtDesc(userId, pageable)

    override fun findByUserIdAndProjectExternalIdOrderByLastMessageAtDesc(
        userId: Long,
        projectExternalId: String,
        pageable: Pageable,
    ): Page<WikiChatSession> = jpa.findByUserIdAndProjectExternalIdOrderByLastMessageAtDesc(userId, projectExternalId, pageable)

    override fun findByUserIdAndSessionTypeOrderByLastMessageAtDesc(
        userId: Long,
        sessionType: WikiChatSessionType,
        pageable: Pageable,
    ): Page<WikiChatSession> = jpa.findByUserIdAndSessionTypeOrderByLastMessageAtDesc(userId, sessionType, pageable)

    override fun save(session: WikiChatSession): WikiChatSession = jpa.save(session)

    override fun delete(session: WikiChatSession) = jpa.delete(session)

    override fun deleteByExpiresAtBefore(now: LocalDateTime) = jpa.deleteByExpiresAtBefore(now)
}

@Repository
class WikiChatMessageRepositoryAdapter(
    private val jpa: WikiChatMessageJpaRepository,
) : WikiChatMessageRepository {
    override fun findBySessionOrderByCreatedAtAsc(session: WikiChatSession): List<WikiChatMessage> =
        jpa.findBySessionOrderByCreatedAtAsc(session)

    override fun findTop20BySessionOrderByCreatedAtDesc(session: WikiChatSession): List<WikiChatMessage> =
        jpa.findTop20BySessionOrderByCreatedAtDesc(session)

    override fun countBySession(session: WikiChatSession): Int = jpa.countBySession(session)

    override fun countMapForSessions(sessions: List<WikiChatSession>): Map<Long, Long> =
        jpa.countBySessionsGrouped(sessions).associate { row -> (row[0] as Long) to (row[1] as Long) }

    override fun save(message: WikiChatMessage): WikiChatMessage = jpa.save(message)
}

@Repository
class WikiSectionChunkRepositoryAdapter(
    private val jpa: WikiSectionChunkJpaRepository,
    private val search: WikiSectionChunkSearch,
) : WikiSectionChunkRepository {
    override fun findByProjectExternalId(projectExternalId: String): List<WikiSectionChunk> = jpa.findByProjectExternalId(projectExternalId)

    override fun findAllSummaryChunks(): List<WikiSectionChunk> = jpa.findAllSummaryChunks()

    override fun deleteByProjectExternalId(projectExternalId: String) = jpa.deleteByProjectExternalId(projectExternalId)

    override fun findSimilarChunksWithScore(
        projectExternalId: String,
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow> = search.findSimilarChunksWithScore(projectExternalId, queryEmbedding, limit)

    override fun findSimilarChunksGlobalWithScore(
        queryEmbedding: String,
        limit: Int,
    ): List<ScoredChunkRow> = search.findSimilarChunksGlobalWithScore(queryEmbedding, limit)

    override fun findLexicalCandidates(
        projectExternalId: String,
        question: String,
        limit: Int,
    ): List<ScoredChunkRow> = search.findLexicalCandidates(projectExternalId, question, limit)
}
