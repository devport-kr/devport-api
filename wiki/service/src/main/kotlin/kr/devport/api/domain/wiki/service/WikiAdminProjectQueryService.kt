package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.repository.ProjectRepository
import kr.devport.api.domain.wiki.dto.response.WikiAdminProjectSummaryResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WikiAdminProjectQueryService(
    private val projectRepository: ProjectRepository,
) {
    fun listProjects(): List<WikiAdminProjectSummaryResponse> = projectRepository.findAllForWikiAdmin().map { it.toSummary() }

    private fun Project.toSummary(): WikiAdminProjectSummaryResponse =
        WikiAdminProjectSummaryResponse(
            projectId = id,
            projectExternalId = externalId,
            fullName = fullName,
            stars = stars ?: 0,
            language = language?.takeUnless { it.isBlank() } ?: "Unknown",
        )
}
