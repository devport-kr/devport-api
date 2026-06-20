package kr.devport.api.domain.port.service.admin

import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.repository.ProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@Service
class ProjectAdminService(
    private val projectRepository: ProjectRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createProject(request: ProjectCreateRequest): Project {
        val fullName = request.fullName.trim()
        val externalId = "github:${fullName.lowercase()}"

        if (projectRepository.findByExternalId(externalId).isPresent) {
            throw IllegalStateException("Project already exists: $fullName")
        }

        val name = if (fullName.contains("/")) fullName.substring(fullName.indexOf("/") + 1) else fullName
        val parts = fullName.split("/", limit = 2)
        val gh = if (parts.size == 2) fetchGitHubRepo(parts[0], parts[1]) else emptyMap()

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

    private fun fetchGitHubRepo(
        owner: String,
        repo: String,
    ): Map<String, Any?> {
        try {
            val restTemplate = RestTemplate()
            val response =
                restTemplate.exchange(
                    "https://api.github.com/repos/$owner/$repo",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<Map<String, Any?>>() {},
                )
            return response.body ?: emptyMap()
        } catch (e: Exception) {
            log.warn("Failed to fetch GitHub repo {}/{}: {}", owner, repo, e.message)
            return emptyMap()
        }
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
