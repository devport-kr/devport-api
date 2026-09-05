@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.entity.ProjectComment
import kr.devport.api.domain.port.entity.ProjectCommentVote
import kr.devport.api.domain.port.entity.ProjectEvent
import kr.devport.api.domain.port.enums.EventType
import kr.devport.api.domain.port.infrastructure.ProjectCommentRepository
import kr.devport.api.domain.port.infrastructure.ProjectCommentVoteRepository
import kr.devport.api.domain.port.infrastructure.ProjectEventRepository
import kr.devport.api.domain.port.infrastructure.ProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class ProjectRepositoryAdapter(
    private val jpa: ProjectJpaRepository,
) : ProjectRepository {
    override fun findById(id: Long): Optional<Project> = jpa.findById(id)

    override fun findByExternalId(externalId: String): Optional<Project> = jpa.findByExternalId(externalId)

    override fun findAllForWikiAdmin(): List<Project> = jpa.findAllForWikiAdmin()

    override fun findAll(sort: Sort): MutableList<Project> = jpa.findAll(sort)

    override fun findHotProjects(limit: Int): List<Project> = jpa.findHotProjects(limit)

    override fun save(project: Project): Project = jpa.save(project)
}

@Repository
class ProjectCommentRepositoryAdapter(
    private val jpa: ProjectCommentJpaRepository,
) : ProjectCommentRepository {
    override fun findByExternalId(externalId: String): Optional<ProjectComment> = jpa.findByExternalId(externalId)

    override fun findAllByProjectExternalId(projectExternalId: String): List<ProjectComment> =
        jpa.findAllByProjectExternalId(projectExternalId)

    override fun countByProjectExternalId(projectExternalId: String): Long = jpa.countByProjectExternalId(projectExternalId)

    override fun save(comment: ProjectComment): ProjectComment = jpa.save(comment)
}

@Repository
class ProjectCommentVoteRepositoryAdapter(
    private val jpa: ProjectCommentVoteJpaRepository,
) : ProjectCommentVoteRepository {
    override fun findByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    ): Optional<ProjectCommentVote> = jpa.findByComment_ExternalIdAndUserId(commentExternalId, userId)

    override fun calculateVoteScore(commentExternalId: String): Int? = jpa.calculateVoteScore(commentExternalId)

    override fun deleteByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    ) = jpa.deleteByComment_ExternalIdAndUserId(commentExternalId, userId)

    override fun save(vote: ProjectCommentVote): ProjectCommentVote = jpa.save(vote)

    override fun delete(vote: ProjectCommentVote) = jpa.delete(vote)
}

@Repository
class ProjectEventRepositoryAdapter(
    private val jpa: ProjectEventJpaRepository,
) : ProjectEventRepository {
    override fun findByProject_ExternalId(
        projectExternalId: String,
        pageable: Pageable,
    ): Page<ProjectEvent> = jpa.findByProject_ExternalId(projectExternalId, pageable)

    override fun findByProjectAndEventType(
        projectExternalId: String,
        eventType: EventType,
        pageable: Pageable,
    ): Page<ProjectEvent> = jpa.findByProjectAndEventType(projectExternalId, eventType, pageable)

    override fun findTop10ByImpactScoreGreaterThanEqualOrderByReleasedAtDesc(impactThreshold: Int): List<ProjectEvent> =
        jpa.findTop10ByImpactScoreGreaterThanEqualOrderByReleasedAtDesc(impactThreshold)
}
