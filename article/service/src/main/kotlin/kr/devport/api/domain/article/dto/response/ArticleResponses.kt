package kr.devport.api.domain.article.dto.response

import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import java.time.LocalDateTime

data class ArticleResponse(
    val id: Long? = null,
    val externalId: String? = null,
    val itemType: ItemType? = null,
    val source: String? = null,
    val category: Category? = null,
    val summaryKoTitle: String? = null,
    val summaryKoBody: String? = null,
    val titleEn: String? = null,
    val url: String? = null,
    val score: Int? = null,
    val tags: List<String>? = null,
    val createdAtSource: LocalDateTime? = null,
    val metadata: ArticleMetadataResponse? = null,
)

data class ArticleDetailResponse(
    val externalId: String? = null,
    val itemType: ItemType? = null,
    val source: String? = null,
    val category: Category? = null,
    val summaryKoTitle: String? = null,
    val summaryKoBody: String? = null,
    val titleEn: String? = null,
    val url: String? = null,
    val score: Int? = null,
    val tags: List<String>? = null,
    val createdAtSource: LocalDateTime? = null,
    val metadata: ArticleMetadataResponse? = null,
)

data class ArticleMetadataResponse(
    val stars: Int? = null,
    val comments: Int? = null,
    val upvotes: Int? = null,
    val readTime: String? = null,
    val language: String? = null,
)

data class ArticlePageResponse(
    val content: List<ArticleResponse>? = null,
    val totalElements: Long? = null,
    val totalPages: Int? = null,
    val currentPage: Int? = null,
    val hasMore: Boolean? = null,
)

data class TrendingTickerResponse(
    val id: Long? = null,
    val externalId: String? = null,
    val summaryKoTitle: String? = null,
    val createdAtSource: LocalDateTime? = null,
)

data class ArticleAutocompleteResponse(
    val externalId: String? = null,
    val summaryKoTitle: String? = null,
    val source: String? = null,
    val category: String? = null,
    val matchType: MatchType? = null,
    val score: Int? = null,
) {
    enum class MatchType {
        TITLE,
        BODY,
    }
}

data class ArticleAutocompleteListResponse(
    val suggestions: List<ArticleAutocompleteResponse>? = null,
    val totalMatches: Long? = null,
)
