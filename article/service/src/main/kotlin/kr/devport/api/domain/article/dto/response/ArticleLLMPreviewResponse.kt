package kr.devport.api.domain.article.dto.response

/**
 * Admin LLM preview result. The `technical` field name is preserved from the original Lombok DTO,
 * whose primitive `boolean isTechnical` getter serialized to JSON as "technical".
 */
data class ArticleLLMPreviewResponse(
    val technical: Boolean = false,
    val titleKo: String? = null,
    val summaryKo: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val url: String? = null,
    val titleEn: String? = null,
    val source: String? = null,
)
