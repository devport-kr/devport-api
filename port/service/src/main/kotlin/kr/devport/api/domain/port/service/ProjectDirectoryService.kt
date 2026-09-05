package kr.devport.api.domain.port.service

import kr.devport.api.domain.port.ProjectView
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.infrastructure.ProjectDirectory
import kr.devport.api.domain.port.infrastructure.ProjectRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Implements the cross-domain inbound port: resolves projects to [ProjectView] for other domains. */
@Service
@Transactional(readOnly = true)
class ProjectDirectoryService(
    private val projectRepository: ProjectRepository,
) : ProjectDirectory {
    override fun listAllByStarsDesc(): List<ProjectView> =
        projectRepository.findAll(Sort.by(Sort.Direction.DESC, "stars")).map { it.toView() }

    override fun listForWikiAdmin(): List<ProjectView> = projectRepository.findAllForWikiAdmin().map { it.toView() }

    override fun findByExternalId(externalId: String): ProjectView? =
        projectRepository.findByExternalId(externalId).map { it.toView() }.orElse(null)
}

private fun Project.toView(): ProjectView =
    ProjectView(
        id = id,
        externalId = externalId,
        fullName = fullName,
        description = description,
        stars = stars,
        forks = forks,
        language = language,
    )
