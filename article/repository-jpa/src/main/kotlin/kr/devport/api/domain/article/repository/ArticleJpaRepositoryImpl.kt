package kr.devport.api.domain.article.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.entity.QArticle
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.util.StringUtils.hasText
import java.time.LocalDateTime

@Repository
class ArticleJpaRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ArticleRepositoryCustom {
    private val article = QArticle.article

    override fun searchWithCondition(
        condition: ArticleSearchCondition,
        pageable: Pageable,
    ): Page<Article> {
        val content =
            queryFactory
                .selectFrom(article)
                .where(
                    categoryEq(condition.category),
                    sourceEq(condition.source),
                    itemTypeEq(condition.itemType),
                    keywordContains(condition.keyword),
                    scoreGoe(condition.minScore),
                    scoreLoe(condition.maxScore),
                    createdAtGoe(condition.createdAfter),
                    createdAtLoe(condition.createdBefore),
                    tagsContainsAny(condition.tags),
                )
                .orderBy(article.score.desc(), article.createdAtSource.desc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val total =
            queryFactory
                .select(article.count())
                .from(article)
                .where(
                    categoryEq(condition.category),
                    sourceEq(condition.source),
                    itemTypeEq(condition.itemType),
                    keywordContains(condition.keyword),
                    scoreGoe(condition.minScore),
                    scoreLoe(condition.maxScore),
                    createdAtGoe(condition.createdAfter),
                    createdAtLoe(condition.createdBefore),
                    tagsContainsAny(condition.tags),
                )
                .fetchOne()

        return PageImpl(content, pageable, total ?: 0L)
    }

    // ========== BooleanExpression helpers (null condition is ignored by QueryDSL) ==========

    private fun categoryEq(category: Category?): BooleanExpression? = category?.let { article.category.eq(it) }

    private fun sourceEq(source: String?): BooleanExpression? = if (hasText(source)) article.source.eq(source) else null

    private fun itemTypeEq(itemType: ItemType?): BooleanExpression? = itemType?.let { article.itemType.eq(it) }

    private fun keywordContains(keyword: String?): BooleanExpression? {
        if (!hasText(keyword)) {
            return null
        }
        return article.summaryKoTitle
            .containsIgnoreCase(keyword)
            .or(article.titleEn.containsIgnoreCase(keyword))
    }

    private fun scoreGoe(minScore: Int?): BooleanExpression? = minScore?.let { article.score.goe(it) }

    private fun scoreLoe(maxScore: Int?): BooleanExpression? = maxScore?.let { article.score.loe(it) }

    private fun createdAtGoe(from: LocalDateTime?): BooleanExpression? = from?.let { article.createdAtSource.goe(it) }

    private fun createdAtLoe(to: LocalDateTime?): BooleanExpression? = to?.let { article.createdAtSource.loe(it) }

    private fun tagsContainsAny(tags: List<String>?): BooleanExpression? {
        if (tags.isNullOrEmpty()) {
            return null
        }
        return article.tags.any().`in`(tags)
    }

    // ========== Autocomplete / full-text search ==========

    override fun searchAutocomplete(
        query: String,
        limit: Int,
    ): List<Article> {
        if (!hasText(query) || query.trim().length < 2) {
            return emptyList()
        }
        val searchTerm = query.trim()

        val priorityOrder =
            CaseBuilder()
                .`when`(article.summaryKoTitle.containsIgnoreCase(searchTerm))
                .then(1)
                .otherwise(2)

        return queryFactory
            .selectFrom(article)
            .where(
                article.summaryKoTitle
                    .containsIgnoreCase(searchTerm)
                    .or(article.summaryKoBody.containsIgnoreCase(searchTerm)),
            )
            .orderBy(priorityOrder.asc(), article.createdAtSource.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun searchFulltext(
        query: String,
        pageable: Pageable,
    ): Page<Article> {
        if (!hasText(query) || query.trim().length < 2) {
            return PageImpl(emptyList(), pageable, 0L)
        }
        val searchTerm = query.trim()

        val priorityOrder =
            CaseBuilder()
                .`when`(article.summaryKoTitle.containsIgnoreCase(searchTerm))
                .then(1)
                .otherwise(2)

        val searchCondition =
            article.summaryKoTitle
                .containsIgnoreCase(searchTerm)
                .or(article.summaryKoBody.containsIgnoreCase(searchTerm))

        val content =
            queryFactory
                .selectFrom(article)
                .where(searchCondition)
                .orderBy(priorityOrder.asc(), article.createdAtSource.desc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val total = countFulltextMatches(query)

        return PageImpl(content, pageable, total)
    }

    override fun countFulltextMatches(query: String): Long {
        if (!hasText(query) || query.trim().length < 2) {
            return 0L
        }
        val searchTerm = query.trim()

        return queryFactory
            .select(article.count())
            .from(article)
            .where(
                article.summaryKoTitle
                    .containsIgnoreCase(searchTerm)
                    .or(article.summaryKoBody.containsIgnoreCase(searchTerm)),
            )
            .fetchOne() ?: 0L
    }
}
