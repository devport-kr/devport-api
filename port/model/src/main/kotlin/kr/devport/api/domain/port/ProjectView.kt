package kr.devport.api.domain.port

/**
 * Cross-domain view of a project — the shape other domains may consume (via ProjectDirectory).
 * Pure domain model: no JPA. Other domains reference projects by externalId and resolve to this.
 */
data class ProjectView(
    val id: Long?,
    val externalId: String?,
    val fullName: String?,
    val description: String?,
    val stars: Int?,
    val forks: Int?,
    val language: String?,
)
