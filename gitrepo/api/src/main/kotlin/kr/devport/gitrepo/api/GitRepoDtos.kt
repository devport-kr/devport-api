package kr.devport.gitrepo.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import kr.devport.gitrepo.Category
import kr.devport.gitrepo.GitRepo
import kr.devport.gitrepo.GitRepoPage
import kr.devport.gitrepo.service.GitRepoCreateCommand
import kr.devport.gitrepo.service.GitRepoUpdateCommand
import java.time.LocalDateTime

@Schema(description = "GitHub Repository response")
data class GitRepoResponse(
    val id: Long?,
    val fullName: String,
    val url: String,
    val description: String?,
    val language: String?,
    val stars: Int?,
    val forks: Int?,
    val starsThisWeek: Int?,
    val summaryKoTitle: String?,
    val summaryKoBody: String?,
    val category: Category?,
    val score: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Schema(description = "Paginated git repository response")
data class GitRepoPageResponse(
    val content: List<GitRepoResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val hasMore: Boolean,
)

@Schema(description = "Create git repository request")
data class GitRepoCreateRequest(
    @field:NotBlank(message = "Full name is required")
    val fullName: String,
    @field:NotBlank(message = "URL is required")
    val url: String,
    val description: String? = null,
    val language: String? = null,
    val stars: Int? = null,
    val forks: Int? = null,
    val starsThisWeek: Int? = null,
    val summaryKoTitle: String? = null,
    val summaryKoBody: String? = null,
    val category: Category? = null,
    val score: Int = 0,
)

@Schema(description = "Update git repository request (null fields are left unchanged)")
data class GitRepoUpdateRequest(
    val fullName: String? = null,
    val url: String? = null,
    val description: String? = null,
    val language: String? = null,
    val stars: Int? = null,
    val forks: Int? = null,
    val starsThisWeek: Int? = null,
    val summaryKoTitle: String? = null,
    val summaryKoBody: String? = null,
    val category: Category? = null,
    val score: Int? = null,
)

internal fun GitRepo.toResponse(): GitRepoResponse =
    GitRepoResponse(
        id = id,
        fullName = fullName,
        url = url,
        description = description,
        language = language,
        stars = stars,
        forks = forks,
        starsThisWeek = starsThisWeek,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        category = category,
        score = score,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun GitRepoPage.toResponse(): GitRepoPageResponse =
    GitRepoPageResponse(
        content = content.map { it.toResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        currentPage = currentPage,
        hasMore = hasMore,
    )

internal fun GitRepoCreateRequest.toCommand(): GitRepoCreateCommand =
    GitRepoCreateCommand(
        fullName = fullName,
        url = url,
        description = description,
        language = language,
        stars = stars,
        forks = forks,
        starsThisWeek = starsThisWeek,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        category = category,
        score = score,
    )

internal fun GitRepoUpdateRequest.toCommand(): GitRepoUpdateCommand =
    GitRepoUpdateCommand(
        fullName = fullName,
        url = url,
        description = description,
        language = language,
        stars = stars,
        forks = forks,
        starsThisWeek = starsThisWeek,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        category = category,
        score = score,
    )
