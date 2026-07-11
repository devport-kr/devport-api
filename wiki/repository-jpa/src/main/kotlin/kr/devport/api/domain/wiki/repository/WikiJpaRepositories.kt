package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.wiki.entity.WikiChatMessage
import kr.devport.api.domain.wiki.entity.WikiChatSession
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

// Spring Data interfaces — internal to this adapter module. The core depends on the ports in
// :wiki:infrastructure; the @Repository adapters in WikiRepositoryAdapters.kt bridge the two.

interface WikiChatSessionJpaRepository : JpaRepository<WikiChatSession, Long> {
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

    fun deleteByExpiresAtBefore(now: LocalDateTime)
}

interface WikiChatMessageJpaRepository : JpaRepository<WikiChatMessage, Long> {
    fun findBySessionOrderByCreatedAtAsc(session: WikiChatSession): List<WikiChatMessage>

    fun findTop20BySessionOrderByCreatedAtDesc(session: WikiChatSession): List<WikiChatMessage>

    fun countBySession(session: WikiChatSession): Int

    @Query("SELECT m.session.id, COUNT(m) FROM WikiChatMessage m WHERE m.session IN :sessions GROUP BY m.session.id")
    fun countBySessionsGrouped(
        @Param("sessions") sessions: List<WikiChatSession>,
    ): List<Array<Any>>
}

interface WikiSectionChunkJpaRepository : JpaRepository<WikiSectionChunk, Long> {
    fun findByProjectExternalId(projectExternalId: String): List<WikiSectionChunk>

    @Query("SELECT c FROM WikiSectionChunk c WHERE c.chunkType = 'summary' ORDER BY c.id ASC")
    fun findAllSummaryChunks(): List<WikiSectionChunk>

    fun deleteByProjectExternalId(projectExternalId: String)
}
