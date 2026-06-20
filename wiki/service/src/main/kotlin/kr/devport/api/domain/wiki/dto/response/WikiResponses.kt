package kr.devport.api.domain.wiki.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import java.time.LocalDateTime

data class RelatedProjectResponse(
    val projectExternalId: String?,
    val fullName: String?,
    val description: String?,
    val relevanceReason: String?,
    val stars: Int,
)

data class WikiGlobalChatResponse(
    val answer: String?,
    val relatedProjects: List<RelatedProjectResponse>?,
    val hasRelatedProjects: Boolean,
    val sessionId: String?,
)

data class WikiMessageResponse(
    val role: String?,
    val content: String?,
    @get:JsonProperty("isClarification")
    val isClarification: Boolean,
    val createdAt: LocalDateTime?,
)

data class WikiSessionListResponse(
    val sessions: List<WikiSessionResponse>?,
    val totalPages: Int,
    val totalElements: Long,
    val page: Int,
    val size: Int,
)

data class WikiSessionResponse(
    val sessionId: String?,
    val title: String?,
    val sessionType: String?,
    val projectExternalId: String?,
    val createdAt: LocalDateTime?,
    val lastMessageAt: LocalDateTime?,
    val messageCount: Int,
)

data class WikiAdminProjectSummaryResponse(
    val projectId: Long? = null,
    val projectExternalId: String? = null,
    val fullName: String? = null,
    val stars: Int? = null,
    val language: String? = null,
)

data class WikiChatResponse(
    val answer: String? = null,
    @get:JsonProperty("isClarification")
    val isClarification: Boolean = false,
    val sessionId: String? = null,
) {
    companion object {
        @JvmStatic
        fun from(
            result: WikiChatResult,
            sessionId: String?,
        ): WikiChatResponse =
            WikiChatResponse(
                answer = result.answer,
                isClarification = result.isClarification,
                sessionId = sessionId,
            )
    }
}

data class WikiProjectListResponse(
    val projects: List<ProjectSummary>? = null,
) {
    data class ProjectSummary(
        val projectExternalId: String? = null,
        val fullName: String? = null,
        val description: String? = null,
        val stars: Int? = null,
        val language: String? = null,
        val summary: String? = null,
    )
}
