package kr.devport.api.domain.article.dto.request.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import java.time.LocalDateTime

data class ArticleMetadataRequest(
    val stars: Int? = null,
    val comments: Int? = null,
    val upvotes: Int? = null,
    val readTime: String? = null,
    val language: String? = null,
)

data class ArticleCreateRequest(
    @field:NotNull(message = "Item type is required")
    val itemType: ItemType? = null,
    @field:NotBlank(message = "Source is required")
    val source: String? = null,
    @field:NotNull(message = "Category is required")
    val category: Category? = null,
    @field:NotBlank(message = "Korean summary title is required")
    val summaryKoTitle: String? = null,
    val summaryKoBody: String? = null,
    @field:NotBlank(message = "English title is required")
    val titleEn: String? = null,
    @field:NotBlank(message = "URL is required")
    val url: String? = null,
    @field:NotNull(message = "Score is required")
    val score: Int? = null,
    val tags: List<String>? = null,
    @field:NotNull(message = "Source creation date is required")
    val createdAtSource: LocalDateTime? = null,
    val metadata: ArticleMetadataRequest? = null,
)

data class ArticleLLMCreateRequest(
    @field:NotBlank(message = "English title is required")
    val titleEn: String? = null,
    @field:NotBlank(message = "URL is required")
    val url: String? = null,
    @field:NotBlank(message = "Content is required")
    val content: String? = null,
    @field:NotBlank(message = "Source is required")
    val source: String? = null,
    val itemType: ItemType = ItemType.BLOG,
    val tags: List<String>? = null,
    val metadata: ArticleMetadataRequest? = null,
)

data class ArticleUpdateRequest(
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
    val metadata: ArticleMetadataRequest? = null,
)
