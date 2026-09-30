package kr.devport.api.domain.article.infrastructure

import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.entity.ArticleComment
import kr.devport.api.domain.article.enums.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** Out-port: article persistence contract owned by the core (entity-backed; JPA adapter implements it). */
interface ArticleRepository {
    fun findById(id: Long): Article?

    fun findAllByIdIn(ids: Collection<Long>): List<Article>

    fun findByExternalId(externalId: String): Article?

    fun findByCategory(
        category: Category,
        pageable: Pageable,
    ): Page<Article>

    fun findAll(pageable: Pageable): Page<Article>

    fun findAllByOrderByScoreDescCreatedAtSourceDesc(pageable: Pageable): List<Article>

    fun save(article: Article): Article

    fun existsById(id: Long): Boolean

    fun deleteById(id: Long)

    // QueryDSL-backed dynamic search
    fun searchWithCondition(
        condition: ArticleSearchCondition,
        pageable: Pageable,
    ): Page<Article>

    /** Exact-count full-text search (title OR body). Used by admin listing. */
    fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article>

    /** Full-text page content only (title OR body, title matches first, then recency); no count. */
    fun searchFulltextContent(
        query: String,
        pageable: Pageable,
    ): List<Article>

    /** Full-text match count, counting at most [cap] rows (cheap for very common terms). */
    fun countFulltextMatches(
        query: String,
        cap: Int,
    ): Long

    /** Title-only autocomplete suggestions, newest first. Lightweight projection (no body, no tags). */
    fun findTitleSuggestions(
        query: String,
        limit: Int,
    ): List<ArticleTitleSuggestionRow>

    /** Title-only match count, counting at most [cap] rows. */
    fun countTitleMatches(
        query: String,
        cap: Int,
    ): Long
}

/** Pure row the autocomplete port speaks — only what the dropdown needs. */
data class ArticleTitleSuggestionRow(
    val externalId: String?,
    val summaryKoTitle: String?,
    val source: String?,
    val category: Category?,
    val score: Int?,
)

/** Out-port: article comment persistence contract. */
interface ArticleCommentRepository {
    fun findByExternalId(externalId: String): ArticleComment?

    fun findAllByArticleExternalId(articleExternalId: String): List<ArticleComment>

    fun countByArticleExternalId(articleExternalId: String): Long

    fun existsByParentCommentId(parentId: Long): Boolean

    fun save(comment: ArticleComment): ArticleComment
}
