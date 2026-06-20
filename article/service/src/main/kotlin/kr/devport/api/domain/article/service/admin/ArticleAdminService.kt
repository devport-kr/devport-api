package kr.devport.api.domain.article.service.admin

import kr.devport.api.domain.article.dto.request.admin.ArticleCreateRequest
import kr.devport.api.domain.article.dto.request.admin.ArticleLLMCreateRequest
import kr.devport.api.domain.article.dto.request.admin.ArticleUpdateRequest
import kr.devport.api.domain.article.dto.response.ArticleLLMPreviewResponse
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.ArticleResponse
import kr.devport.api.domain.article.entity.Article
import kr.devport.api.domain.article.entity.ArticleMetadata
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.repository.ArticleRepository
import kr.devport.api.domain.article.service.toMetadataResponse
import kr.devport.api.domain.common.cache.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ArticleAdminService(
    private val articleRepository: ArticleRepository,
    private val articleLLMService: ArticleLLMService,
) {
    @CacheEvict(cacheNames = [CacheNames.ARTICLES, CacheNames.TRENDING_TICKER], allEntries = true)
    fun createArticle(request: ArticleCreateRequest): ArticleResponse {
        val article =
            Article().apply {
                itemType = request.itemType
                source = request.source
                category = request.category
                summaryKoTitle = request.summaryKoTitle
                summaryKoBody = request.summaryKoBody
                titleEn = request.titleEn
                url = request.url
                score = request.score
                tags = request.tags?.toMutableList() ?: mutableListOf()
                createdAtSource = request.createdAtSource
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        request.metadata?.let { m ->
            article.metadata =
                ArticleMetadata().apply {
                    stars = m.stars
                    comments = m.comments
                    upvotes = m.upvotes
                    readTime = m.readTime
                    language = m.language
                }
        }
        return articleRepository.save(article).toResponse()
    }

    @CacheEvict(cacheNames = [CacheNames.ARTICLES, CacheNames.TRENDING_TICKER], allEntries = true)
    fun updateArticle(
        id: Long,
        request: ArticleUpdateRequest,
    ): ArticleResponse {
        val article =
            articleRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Article not found with id: $id") }

        request.itemType?.let { article.itemType = it }
        request.source?.let { article.source = it }
        request.category?.let { article.category = it }
        request.summaryKoTitle?.let { article.summaryKoTitle = it }
        request.summaryKoBody?.let { article.summaryKoBody = it }
        request.titleEn?.let { article.titleEn = it }
        request.url?.let { article.url = it }
        request.score?.let { article.score = it }
        request.tags?.let { article.tags = it.toMutableList() }
        request.createdAtSource?.let { article.createdAtSource = it }

        request.metadata?.let { reqMeta ->
            val metadata = article.metadata ?: ArticleMetadata().also { article.metadata = it }
            reqMeta.stars?.let { metadata.stars = it }
            reqMeta.comments?.let { metadata.comments = it }
            reqMeta.upvotes?.let { metadata.upvotes = it }
            reqMeta.readTime?.let { metadata.readTime = it }
            reqMeta.language?.let { metadata.language = it }
        }

        article.updatedAt = LocalDateTime.now()
        return articleRepository.save(article).toResponse()
    }

    @CacheEvict(cacheNames = [CacheNames.ARTICLES, CacheNames.TRENDING_TICKER], allEntries = true)
    fun deleteArticle(id: Long) {
        if (!articleRepository.existsById(id)) {
            throw IllegalArgumentException("Article not found with id: $id")
        }
        articleRepository.deleteById(id)
    }

    @CacheEvict(cacheNames = [CacheNames.ARTICLES, CacheNames.TRENDING_TICKER], allEntries = true)
    fun createArticleFromLLM(request: ArticleLLMCreateRequest): ArticleResponse {
        val result =
            articleLLMService.processArticle(
                request.titleEn,
                request.url,
                request.content,
                request.tags,
            )

        val resolvedCategory =
            try {
                Category.valueOf(result.category)
            } catch (e: IllegalArgumentException) {
                Category.OTHER
            }

        val article =
            Article().apply {
                itemType = request.itemType
                source = request.source
                category = resolvedCategory
                summaryKoTitle = result.titleKo
                summaryKoBody = result.summaryKo
                titleEn = request.titleEn
                url = request.url
                score = 100
                tags = result.tags.toMutableList()
                createdAtSource = LocalDateTime.now()
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        request.metadata?.let { m ->
            article.metadata =
                ArticleMetadata().apply {
                    stars = m.stars
                    comments = m.comments
                    upvotes = m.upvotes
                    readTime = m.readTime
                    language = m.language
                }
        }
        return articleRepository.save(article).toResponse()
    }

    fun previewArticleLLM(request: ArticleLLMCreateRequest): ArticleLLMPreviewResponse {
        val result =
            articleLLMService.processArticle(
                request.titleEn,
                request.url,
                request.content,
                request.tags,
            )

        return ArticleLLMPreviewResponse(
            technical = result.isTechnical,
            titleKo = result.titleKo,
            summaryKo = result.summaryKo,
            category = result.category,
            tags = result.tags,
            url = result.url,
            titleEn = request.titleEn,
            source = request.source,
        )
    }

    @Transactional(readOnly = true)
    fun listArticles(
        page: Int,
        size: Int,
        search: String?,
    ): ArticlePageResponse {
        val pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val articlePage =
            if (!search.isNullOrBlank()) {
                articleRepository.searchFulltext(search.trim(), pageRequest)
            } else {
                articleRepository.findAll(pageRequest)
            }
        return ArticlePageResponse(
            content = articlePage.content.map { it.toResponse() },
            totalElements = articlePage.totalElements,
            totalPages = articlePage.totalPages,
            currentPage = articlePage.number,
            hasMore = articlePage.hasNext(),
        )
    }

    // Admin responses intentionally omit externalId to mirror the original Java mapper.
    private fun Article.toResponse(): ArticleResponse =
        ArticleResponse(
            id = id,
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
}
