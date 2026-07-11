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

    /** Autocomplete: title matches prioritized, then recency. Min 2 chars. */
    fun searchAutocomplete(
        query: String,
        limit: Int,
    ): List<Article>

    /** Full-text search over Korean title/body, title matches prioritized, then recency. */
    fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article>

    /** Total full-text match count for the "show all X results" UI. */
    fun countFulltextMatches(query: String): Long
}
