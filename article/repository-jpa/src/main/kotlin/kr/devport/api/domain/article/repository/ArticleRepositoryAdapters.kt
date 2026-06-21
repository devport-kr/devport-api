package kr.devport.api.domain.article.repository

import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.entity.ArticleComment
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.infrastructure.ArticleCommentRepository
import kr.devport.api.domain.article.infrastructure.ArticleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ArticleRepositoryAdapter(
    private val jpa: ArticleJpaRepository,
) : ArticleRepository {
    override fun findById(id: Long): Article? = jpa.findById(id).orElse(null)

    override fun findByExternalId(externalId: String): Article? = jpa.findByExternalId(externalId)

    override fun findByCategory(
        category: Category,
        pageable: Pageable,
    ): Page<Article> = jpa.findByCategory(category, pageable)

    override fun findAll(pageable: Pageable): Page<Article> = jpa.findAll(pageable)

    override fun findAllByOrderByScoreDescCreatedAtSourceDesc(pageable: Pageable): List<Article> =
        jpa.findAllByOrderByScoreDescCreatedAtSourceDesc(pageable)

    override fun save(article: Article): Article = jpa.save(article)

    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun searchWithCondition(
        condition: ArticleSearchCondition,
        pageable: Pageable,
    ): Page<Article> = jpa.searchWithCondition(condition, pageable)

    override fun searchAutocomplete(
        query: String,
        limit: Int,
    ): List<Article> = jpa.searchAutocomplete(query, limit)

    override fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article> = jpa.searchFulltext(query, pageable)

    override fun countFulltextMatches(query: String): Long = jpa.countFulltextMatches(query)
}

@Repository
class ArticleCommentRepositoryAdapter(
    private val jpa: ArticleCommentJpaRepository,
) : ArticleCommentRepository {
    override fun findByExternalId(externalId: String): ArticleComment? = jpa.findByExternalId(externalId)

    override fun findAllByArticleExternalId(articleExternalId: String): List<ArticleComment> =
        jpa.findAllByArticleExternalId(articleExternalId)

    override fun countByArticleExternalId(articleExternalId: String): Long = jpa.countByArticleExternalId(articleExternalId)

    override fun existsByParentCommentId(parentId: Long): Boolean = jpa.existsByParentComment_Id(parentId)

    override fun save(comment: ArticleComment): ArticleComment = jpa.save(comment)
}
