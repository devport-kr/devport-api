package kr.devport.api.domain.article.service

import kr.devport.api.domain.article.dto.response.ArticleAutocompleteResponse
import kr.devport.api.domain.article.dto.response.ArticleDetailResponse
import kr.devport.api.domain.article.dto.response.ArticleMetadataResponse
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.ArticleResponse
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.entity.ArticleMetadata
import kr.devport.api.domain.article.infrastructure.ArticleTitleSuggestionRow
import org.springframework.data.domain.Page

internal fun ArticleMetadata?.toMetadataResponse(): ArticleMetadataResponse? =
    this?.let {
        ArticleMetadataResponse(
            stars = it.stars,
            comments = it.comments,
            upvotes = it.upvotes,
            readTime = it.readTime,
            language = it.language,
        )
    }

internal fun Article.toArticleResponse(): ArticleResponse =
    ArticleResponse(
        id = id,
        externalId = externalId,
        itemType = itemType,
        source = source,
        category = category,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        titleEn = titleEn,
        url = url,
        score = score,
        tags = tags.toList(),
        createdAtSource = createdAtSource,
        metadata = metadata.toMetadataResponse(),
    )

internal fun Article.toDetailResponse(): ArticleDetailResponse =
    ArticleDetailResponse(
        externalId = externalId,
        itemType = itemType,
        source = source,
        category = category,
        summaryKoTitle = summaryKoTitle,
        summaryKoBody = summaryKoBody,
        titleEn = titleEn,
        url = url,
        score = score,
        tags = tags.toList(),
        createdAtSource = createdAtSource,
        metadata = metadata.toMetadataResponse(),
    )

internal fun Article.toTrendingTickerResponse(): TrendingTickerResponse =
    TrendingTickerResponse(
        id = id,
        externalId = externalId,
        summaryKoTitle = summaryKoTitle,
        createdAtSource = createdAtSource,
    )

internal fun ArticleTitleSuggestionRow.toAutocompleteResponse(): ArticleAutocompleteResponse =
    ArticleAutocompleteResponse(
        externalId = externalId,
        summaryKoTitle = summaryKoTitle,
        source = source,
        category = category?.name,
        // Autocomplete matches titles only.
        matchType = ArticleAutocompleteResponse.MatchType.TITLE,
        score = score,
    )

internal fun Page<Article>.toPageResponse(): ArticlePageResponse =
    ArticlePageResponse(
        content = content.map { it.toArticleResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        currentPage = number,
        hasMore = hasNext(),
    )
