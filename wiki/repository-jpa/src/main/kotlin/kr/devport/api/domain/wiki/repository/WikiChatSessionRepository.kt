package kr.devport.api.domain.wiki.repository

import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.wiki.entity.WikiChatSession
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface WikiChatSessionRepository : JpaRepository<WikiChatSession, Long> {
    @EntityGraph(attributePaths = ["user"])
    fun findByExternalId(externalId: String): Optional<WikiChatSession>

    @EntityGraph(attributePaths = ["user"])
    fun findByUserOrderByLastMessageAtDesc(
        user: User,
        pageable: Pageable,
    ): Page<WikiChatSession>

    @EntityGraph(attributePaths = ["user"])
    fun findByUserAndProjectExternalIdOrderByLastMessageAtDesc(
        user: User,
        projectExternalId: String,
        pageable: Pageable,
    ): Page<WikiChatSession>

    @EntityGraph(attributePaths = ["user"])
    fun findByUserAndSessionTypeOrderByLastMessageAtDesc(
        user: User,
        sessionType: WikiChatSessionType,
        pageable: Pageable,
    ): Page<WikiChatSession>

    fun deleteByExpiresAtBefore(now: LocalDateTime)
}
