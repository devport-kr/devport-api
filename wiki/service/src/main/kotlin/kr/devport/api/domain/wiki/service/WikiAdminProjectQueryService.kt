package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.port.ProjectView
import kr.devport.api.domain.port.infrastructure.ProjectDirectory
import kr.devport.api.domain.wiki.dto.response.WikiAdminProjectSummaryResponse
import org.springframework.stereotype.Service

@Service
class WikiAdminProjectQueryService(
    private val projectDirectory: ProjectDirectory,
) {
    fun listProjects(): List<WikiAdminProjectSummaryResponse> = projectDirectory.listForWikiAdmin().map { it.toSummary() }

    private fun ProjectView.toSummary(): WikiAdminProjectSummaryResponse =
        WikiAdminProjectSummaryResponse(
            projectId = id,
            projectExternalId = externalId,
            fullName = fullName,
            stars = stars ?: 0,
            language = language?.takeUnless { it.isBlank() } ?: "Unknown",
        )
}
