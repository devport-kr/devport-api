package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.wiki.entity.WikiChatMessage
import kr.devport.api.domain.wiki.entity.WikiChatSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WikiChatMessageRepository : JpaRepository<WikiChatMessage, Long> {
    fun findBySessionOrderByCreatedAtAsc(session: WikiChatSession): List<WikiChatMessage>

    fun findTop20BySessionOrderByCreatedAtDesc(session: WikiChatSession): List<WikiChatMessage>

    fun countBySession(session: WikiChatSession): Int

    @Query("SELECT m.session.id, COUNT(m) FROM WikiChatMessage m WHERE m.session IN :sessions GROUP BY m.session.id")
    fun countBySessionsGrouped(
        @Param("sessions") sessions: List<WikiChatSession>,
    ): List<Array<Any>>

    fun countMapForSessions(sessions: List<WikiChatSession>): Map<Long, Long> =
        countBySessionsGrouped(sessions).associate { row -> (row[0] as Long) to (row[1] as Long) }
}
