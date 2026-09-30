package kr.devport.api.domain.article.repository

import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.entity.Article
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ArticleRepositoryCustom {
    /** Type-safe dynamic search with 8 optional filters + keyword (QueryDSL). */
    fun searchWithCondition(
        condition: ArticleSearchCondition,
        pageable: Pageable,
    ): Page<Article>

    /** Full-text search over Korean title/body, title matches prioritized, then recency. */
    fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article>

    /** Full-text page content only (no count query). */
    fun searchFulltextContent(
        query: String,
        pageable: Pageable,
    ): List<Article>

    /** Exact full-text match count (admin listing). */
    fun countFulltextMatches(query: String): Long
}
