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

    fun searchAutocomplete(
        query: String,
        limit: Int,
    ): List<Article>

    fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article>

    fun countFulltextMatches(query: String): Long
}

/** Out-port: article comment persistence contract. */
interface ArticleCommentRepository {
    fun findByExternalId(externalId: String): ArticleComment?

    fun findAllByArticleExternalId(articleExternalId: String): List<ArticleComment>

    fun countByArticleExternalId(articleExternalId: String): Long

    fun existsByParentCommentId(parentId: Long): Boolean

    fun save(comment: ArticleComment): ArticleComment
}
