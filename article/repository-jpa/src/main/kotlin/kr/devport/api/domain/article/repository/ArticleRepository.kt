package kr.devport.api.domain.article.repository

import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.enums.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ArticleRepository :
    JpaRepository<Article, Long>,
    ArticleRepositoryCustom {
    fun findByExternalId(externalId: String): Article?

    fun findByCategory(
        category: Category,
        pageable: Pageable,
    ): Page<Article>

    fun findBySource(
        source: String,
        pageable: Pageable,
    ): Page<Article>

    fun findBySourceOrderByScoreDesc(
        source: String,
        pageable: Pageable,
    ): Page<Article>

    fun findAllByOrderByScoreDescCreatedAtSourceDesc(pageable: Pageable): List<Article>
}
