package kr.devport.api.domain.article.infrastructure

/**
 * Out-port: translates/condenses an English article into a Korean rendering.
 * The OpenAI adapter in :article:adapter-openai implements it; the core only sees this contract.
 */
interface ArticleTranslator {
    fun processArticle(
        titleEn: String?,
        url: String?,
        content: String?,
        tags: List<String>?,
    ): LLMArticleResult
}

data class LLMArticleResult(
    val isTechnical: Boolean,
    val titleKo: String,
    val summaryKo: String,
    val category: String,
    val tags: List<String>,
    val url: String?,
)
