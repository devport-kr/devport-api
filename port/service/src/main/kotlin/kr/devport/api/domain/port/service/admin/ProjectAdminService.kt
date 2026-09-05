package kr.devport.api.domain.port.service.admin

import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.infrastructure.GitHubRepoFetcher
import kr.devport.api.domain.port.infrastructure.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProjectAdminService(
    private val projectRepository: ProjectRepository,
    private val gitHubRepoFetcher: GitHubRepoFetcher,
) {
    @Transactional
    fun createProject(request: ProjectCreateRequest): Project {
        val fullName = request.fullName.trim()
        val externalId = "github:${fullName.lowercase()}"

        if (projectRepository.findByExternalId(externalId).isPresent) {
            throw IllegalStateException("Project already exists: $fullName")
        }

        val name = if (fullName.contains("/")) fullName.substring(fullName.indexOf("/") + 1) else fullName
        val parts = fullName.split("/", limit = 2)
        val gh = if (parts.size == 2) gitHubRepoFetcher.fetch(parts[0], parts[1]) else emptyMap()

        val repoUrl = request.repoUrl ?: gh["html_url"] as? String ?: "https://github.com/$fullName"
        val homepageUrl = request.homepageUrl ?: gh["homepage"] as? String
        val description = request.description ?: gh["description"] as? String
        val stars = request.stars ?: toInt(gh["stargazers_count"])
        val forks = request.forks ?: toInt(gh["forks_count"])
        val language = request.language ?: gh["language"] as? String
        val license = request.license ?: extractLicense(gh["license"])

        val now = LocalDateTime.now()
        val project =
            Project().apply {
                this.externalId = externalId
                this.name = name
                this.fullName = fullName
                this.repoUrl = repoUrl
                this.homepageUrl = homepageUrl
                this.description = description
                this.stars = stars
                this.forks = forks
                this.language = language
                this.license = license
                createdAt = now
                updatedAt = now
            }

        return projectRepository.save(project)
    }

    private fun toInt(value: Any?): Int =
        when (value) {
            is Number -> value.toInt()
            else -> 0
        }

    @Suppress("UNCHECKED_CAST")
    private fun extractLicense(licenseObj: Any?): String? {
        if (licenseObj is Map<*, *>) {
            return licenseObj["spdx_id"] as? String
        }
        return null
    }
}
