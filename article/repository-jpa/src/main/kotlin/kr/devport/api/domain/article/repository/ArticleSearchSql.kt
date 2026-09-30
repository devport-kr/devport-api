package kr.devport.api.domain.article.repository

import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.infrastructure.ArticleTitleSuggestionRow
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

/**
 * Search queries that must bypass JPQL: title-only autocomplete (served by the pg_trgm GIN index on
 * summary_ko_title, see sql/manual) and capped counts (`count(*)` over a `LIMIT`ed subquery, so very
 * common terms stop early). Injected into [ArticleRepositoryAdapter], which bridges it to the port.
 */
@Component
class ArticleSearchSql(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val suggestionMapper =
        RowMapper { rs, _ ->
            ArticleTitleSuggestionRow(
                externalId = rs.getString("external_id"),
                summaryKoTitle = rs.getString("summary_ko_title"),
                source = rs.getString("source"),
                category = rs.getString("category")?.let { Category.valueOf(it) },
                score = rs.getInt("score").takeUnless { rs.wasNull() },
            )
        }

    fun findTitleSuggestions(
        query: String,
        limit: Int,
    ): List<ArticleTitleSuggestionRow> {
        if (query.trim().length < MIN_QUERY_LENGTH) return emptyList()
        return jdbcTemplate.query(TITLE_SUGGESTIONS_SQL, suggestionMapper, likePattern(query), limit)
    }

    fun countTitleMatches(
        query: String,
        cap: Int,
    ): Long {
        if (query.trim().length < MIN_QUERY_LENGTH) return 0L
        return jdbcTemplate.queryForObject(TITLE_COUNT_SQL, Long::class.java, likePattern(query), cap) ?: 0L
    }

    fun countFulltextMatches(
        query: String,
        cap: Int,
    ): Long {
        if (query.trim().length < MIN_QUERY_LENGTH) return 0L
        val pattern = likePattern(query.lowercase())
        return jdbcTemplate.queryForObject(FULLTEXT_COUNT_SQL, Long::class.java, pattern, pattern, cap) ?: 0L
    }

    /** `%term%` with LIKE metacharacters escaped by `!` (matches the ESCAPE clause in the SQL below). */
    private fun likePattern(query: String): String {
        val escaped = query.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_")
        return "%$escaped%"
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2

        private const val TITLE_SUGGESTIONS_SQL =
            """
            SELECT external_id, summary_ko_title, source, category, score
            FROM articles
            WHERE summary_ko_title ILIKE ? ESCAPE '!'
            ORDER BY created_at_source DESC
            LIMIT ?
            """

        private const val TITLE_COUNT_SQL =
            """
            SELECT count(*) FROM (
                SELECT 1 FROM articles WHERE summary_ko_title ILIKE ? ESCAPE '!' LIMIT ?
            ) s
            """

        private const val FULLTEXT_COUNT_SQL =
            """
            SELECT count(*) FROM (
                SELECT 1 FROM articles
                WHERE lower(summary_ko_title) LIKE ? ESCAPE '!' OR lower(summary_ko_body) LIKE ? ESCAPE '!'
                LIMIT ?
            ) s
            """
    }
}
