@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.port.infrastructure

import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.entity.ProjectComment
import kr.devport.api.domain.port.entity.ProjectCommentVote
import kr.devport.api.domain.port.entity.ProjectEvent
import kr.devport.api.domain.port.enums.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.Optional

/** Out-ports: port-domain persistence contracts (entity-backed; JPA adapters implement them). */
interface ProjectRepository {
    fun findById(id: Long): Optional<Project>

    fun findByExternalId(externalId: String): Optional<Project>

    fun findAllForWikiAdmin(): List<Project>

    fun findAll(sort: Sort): MutableList<Project>

    fun findHotProjects(limit: Int): List<Project>

    fun save(project: Project): Project
}

interface ProjectCommentRepository {
    fun findByExternalId(externalId: String): Optional<ProjectComment>

    fun findAllByProjectExternalId(projectExternalId: String): List<ProjectComment>

    fun countByProjectExternalId(projectExternalId: String): Long

    fun save(comment: ProjectComment): ProjectComment
}

interface ProjectCommentVoteRepository {
    fun findByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    ): Optional<ProjectCommentVote>

    fun calculateVoteScore(commentExternalId: String): Int?

    fun deleteByComment_ExternalIdAndUserId(
        commentExternalId: String,
        userId: Long,
    )

    fun save(vote: ProjectCommentVote): ProjectCommentVote

    fun delete(vote: ProjectCommentVote)
}

interface ProjectEventRepository {
    fun findByProject_ExternalId(
        projectExternalId: String,
        pageable: Pageable,
    ): Page<ProjectEvent>

    fun findByProjectAndEventType(
        projectExternalId: String,
        eventType: EventType,
        pageable: Pageable,
    ): Page<ProjectEvent>

    fun findTop10ByImpactScoreGreaterThanEqualOrderByReleasedAtDesc(impactThreshold: Int): List<ProjectEvent>
}

/** Out-port: fetches GitHub repository metadata (HTTP adapter implements it). */
interface GitHubRepoFetcher {
    fun fetch(
        owner: String,
        repo: String,
    ): Map<String, Any?>
}
