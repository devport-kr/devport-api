package kr.devport.api.domain.article.service

import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.dto.response.ArticleAutocompleteListResponse
import kr.devport.api.domain.article.dto.response.ArticleDetailResponse
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.infrastructure.ArticleRepository
import kr.devport.api.domain.common.cache.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ArticleService(
    private val articleRepository: ArticleRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.ARTICLES],
        key = "@cacheKeyFactory.articleListKey(#category?.name(), #page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')",
    )
    fun getArticles(
        category: Category?,
        page: Int,
        size: Int,
    ): ArticlePageResponse {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "createdAtSource")),
            )
        val articlePage =
            if (category == null) {
                articleRepository.findAll(pageable)
            } else {
                articleRepository.findByCategory(category, pageable)
            }
        return articlePage.toPageResponse()
    }

    @Cacheable(
        cacheNames = [CacheNames.TRENDING_TICKER],
        key = "@cacheKeyFactory.trendingTickerKey(#limit)",
        unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')",
    )
    fun getTrendingTicker(limit: Int): List<TrendingTickerResponse> {
        val pageable = PageRequest.of(0, limit)
        return articleRepository
            .findAllByOrderByScoreDescCreatedAtSourceDesc(pageable)
            .map { it.toTrendingTickerResponse() }
    }

    fun searchArticles(
        condition: ArticleSearchCondition,
        page: Int,
        size: Int,
    ): ArticlePageResponse {
        val pageable = PageRequest.of(page, size)
        return articleRepository.searchWithCondition(condition, pageable).toPageResponse()
    }

    fun getArticleByExternalId(externalId: String): ArticleDetailResponse {
        val article =
            articleRepository.findByExternalId(externalId)
                ?: throw IllegalArgumentException("Article not found: $externalId")
        return article.toDetailResponse()
    }

    fun searchAutocomplete(query: String): ArticleAutocompleteListResponse {
        val articles = articleRepository.searchAutocomplete(query, 5)
        val totalMatches = articleRepository.countFulltextMatches(query)
        return ArticleAutocompleteListResponse(
            suggestions = articles.map { it.toAutocompleteResponse(query) },
            totalMatches = totalMatches,
        )
    }

    fun searchFulltext(
        query: String,
        page: Int,
        size: Int,
    ): ArticlePageResponse {
        val pageable = PageRequest.of(page, size)
        return articleRepository.searchFulltext(query, pageable).toPageResponse()
    }
}
