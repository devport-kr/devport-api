@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.entity.ProjectComment
import kr.devport.api.domain.port.entity.ProjectCommentVote
import kr.devport.api.domain.port.entity.ProjectEvent
import kr.devport.api.domain.port.enums.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

// Spring Data interfaces — internal to this adapter module. The core depends on the ports in
// :port:infrastructure; the @Repository adapters in PortRepositoryAdapters.kt bridge the two.

interface ProjectJpaRepository :
    JpaRepository<Project, Long>,
    ProjectRepositoryCustom {
    fun findByExternalId(externalId: String): Optional<Project>

    @Query(
        """
        select p
        from Project p
        order by p.stars desc, p.fullName asc
        """,
    )
    fun findAllForWikiAdmin(): List<Project>

    override fun findAll(sort: Sort): MutableList<Project>
}

interface ProjectCommentJpaRepository : JpaRepository<ProjectComment, Long> {
    fun findByExternalId(externalId: String): Optional<ProjectComment>

    @Query(
        """
        SELECT c FROM ProjectComment c
        WHERE c.project.externalId = :projectExternalId
        ORDER BY c.createdAt ASC
        """,
    )
    fun findAllByProjectExternalId(
        @Param("projectExternalId") projectExternalId: String,
    ): List<ProjectComment>

    @Query("SELECT COUNT(c) FROM ProjectComment c WHERE c.project.externalId = :projectExternalId AND c.deleted = false")
    fun countByProjectExternalId(
        @Param("projectExternalId") projectExternalId: String,
    ): Long
}

interface ProjectCommentVoteJpaRepository : JpaRepository<ProjectCommentVote, Long> {
    fun findByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    ): Optional<ProjectCommentVote>

    @Query("SELECT SUM(v.vote) FROM ProjectCommentVote v WHERE v.comment.externalId = :commentExternalId")
    fun calculateVoteScore(
        @Param("commentExternalId") commentExternalId: String,
    ): Int?

    fun deleteByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    )
}

interface ProjectEventJpaRepository : JpaRepository<ProjectEvent, Long> {
    @EntityGraph(attributePaths = ["project"])
    fun findByProject_ExternalId(
        projectExternalId: String,
        pageable: Pageable,
    ): Page<ProjectEvent>

    @EntityGraph(attributePaths = ["project"])
    @Query(
        """
        SELECT e FROM ProjectEvent e
        WHERE e.project.externalId = :projectId
        AND :eventType MEMBER OF e.eventTypes
        """,
    )
    fun findByProjectAndEventType(
        @Param("projectId") projectExternalId: String,
        @Param("eventType") eventType: EventType,
        pageable: Pageable,
    ): Page<ProjectEvent>

    fun findTop10ByImpactScoreGreaterThanEqualOrderByReleasedAtDesc(impactThreshold: Int): List<ProjectEvent>
}
