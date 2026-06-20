package kr.devport.api.domain.port.dto.request.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ProjectCreateRequest(
    @field:NotBlank
    @field:Pattern(regexp = ".+/.+", message = "fullName must be in owner/repo format")
    val fullName: String = "",
    val repoUrl: String? = null,
    val homepageUrl: String? = null,
    val description: String? = null,
    val stars: Int? = null,
    val forks: Int? = null,
    val language: String? = null,
    val license: String? = null,
)
