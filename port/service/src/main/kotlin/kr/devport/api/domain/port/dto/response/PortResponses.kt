package kr.devport.api.domain.port.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import kr.devport.api.domain.auth.UserSummary
import kr.devport.api.domain.port.enums.EventType
import java.time.LocalDate
import java.time.LocalDateTime

data class HotReleaseResponse(
    val id: String? = null,
    val projectName: String? = null,
    val version: String? = null,
    val releasedAt: LocalDate? = null,
    val eventTypes: Set<EventType>? = null,
    val summary: String? = null,
    val impactScore: Int? = null,
    val isSecurity: Boolean? = null,
    val isBreaking: Boolean? = null,
)

data class ProjectCommentAuthorResponse(
    val id: Long? = null,
    val name: String? = null,
    val profileImageUrl: String? = null,
    val flair: String? = null,
    val flairColor: String? = null,
) {
    companion object {
        fun from(user: UserSummary?): ProjectCommentAuthorResponse =
            ProjectCommentAuthorResponse(
                id = user?.id,
                name = user?.name ?: user?.username,
                profileImageUrl = user?.profileImageUrl,
                flair = user?.flair,
                flairColor = user?.flairColor,
            )
    }
}

data class ProjectCommentResponse(
    val id: String? = null,
    val content: String? = null,
    val deleted: Boolean? = null,
    val parentId: String? = null,
    val author: ProjectCommentAuthorResponse? = null,
    val votes: Int? = null,
    val userVote: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @get:JsonProperty("isOwner")
    val isOwner: Boolean? = null,
)

data class ProjectDetailResponse(
    val id: String? = null,
    val name: String? = null,
    val fullName: String? = null,
    val repoUrl: String? = null,
    val homepageUrl: String? = null,
    val description: String? = null,
    val stars: Int? = null,
    val forks: Int? = null,
    val contributors: Int? = null,
    val language: String? = null,
    val languageColor: String? = null,
    val license: String? = null,
    val lastRelease: LocalDate? = null,
    val tags: List<String>? = null,
)

data class ProjectEventResponse(
    val id: String? = null,
    val version: String? = null,
    val releasedAt: LocalDate? = null,
    val eventTypes: Set<EventType>? = null,
    val summary: String? = null,
    val bullets: List<String>? = null,
    val impactScore: Int? = null,
    val isSecurity: Boolean? = null,
    val isBreaking: Boolean? = null,
    val sourceUrl: String? = null,
)

data class ProjectSummaryResponse(
    val id: String? = null,
    val name: String? = null,
    val fullName: String? = null,
    val stars: Int? = null,
    val starsWeekDelta: Int? = null,
    val language: String? = null,
    val languageColor: String? = null,
    val releases30d: Int? = null,
    val sparklineData: List<Int>? = null,
)

data class VoteResponse(
    val votes: Int? = null,
    val userVote: Int? = null,
)
