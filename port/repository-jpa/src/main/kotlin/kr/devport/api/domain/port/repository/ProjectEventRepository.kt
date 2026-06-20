@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.ProjectEvent
import kr.devport.api.domain.port.enums.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectEventRepository : JpaRepository<ProjectEvent, Long> {
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
