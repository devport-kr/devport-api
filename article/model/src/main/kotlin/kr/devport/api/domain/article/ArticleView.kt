package kr.devport.api.domain.article

/**
 * Cross-domain view of an article — the shape other domains may consume (via ArticleDirectory).
 * Pure domain model: no JPA. Other domains reference articles by id and resolve to this.
 */
data class ArticleView(
    val id: Long,
    val externalId: String?,
    val summaryKoTitle: String?,
    val source: String?,
    val category: String?,
    val url: String?,
)
