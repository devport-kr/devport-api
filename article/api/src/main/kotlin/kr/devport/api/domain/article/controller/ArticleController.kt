package kr.devport.api.domain.article.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.devport.api.domain.article.dto.request.ArticleSearchCondition
import kr.devport.api.domain.article.dto.response.ArticleAutocompleteListResponse
import kr.devport.api.domain.article.dto.response.ArticleDetailResponse
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse
import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.enums.ItemType
import kr.devport.api.domain.article.service.ArticleService
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.mypage.service.MyPageService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "Articles", description = "Article and content management endpoints (public)")
@RestController
@RequestMapping("/api/articles")
class ArticleController(
    private val articleService: ArticleService,
    private val myPageService: MyPageService,
) {
    @Operation(summary = "Get articles with pagination")
    @GetMapping
    fun getArticles(
        @RequestParam(required = false) category: Category?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "9") size: Int,
    ): ResponseEntity<ArticlePageResponse> = ResponseEntity.ok(articleService.getArticles(category, page, size))

    @Operation(summary = "Get trending ticker articles")
    @GetMapping("/trending-ticker")
    fun getTrendingTicker(
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<List<TrendingTickerResponse>> = ResponseEntity.ok(articleService.getTrendingTicker(limit))

    @Operation(summary = "Search articles with multiple filters (QueryDSL)")
    @GetMapping("/search")
    fun searchArticles(
        @RequestParam(required = false) category: Category?,
        @RequestParam(required = false) source: String?,
        @RequestParam(required = false) itemType: ItemType?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) minScore: Int?,
        @RequestParam(required = false) maxScore: Int?,
        @RequestParam(required = false) createdAfter: LocalDateTime?,
        @RequestParam(required = false) createdBefore: LocalDateTime?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "9") size: Int,
    ): ResponseEntity<ArticlePageResponse> {
        val condition =
            ArticleSearchCondition(
                category = category,
                source = source,
                itemType = itemType,
                keyword = keyword,
                minScore = minScore,
                maxScore = maxScore,
                createdAfter = createdAfter,
                createdBefore = createdBefore,
                tags = tags,
            )
        return ResponseEntity.ok(articleService.searchArticles(condition, page, size))
    }

    @Operation(summary = "Autocomplete search suggestions (min 2 chars)")
    @GetMapping("/autocomplete")
    fun autocomplete(
        @RequestParam("q") query: String,
    ): ResponseEntity<ArticleAutocompleteListResponse> {
        if (query.trim().length < 2) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(articleService.searchAutocomplete(query))
    }

    @Operation(summary = "Full-text search with pagination (min 2 chars)")
    @GetMapping("/search/fulltext")
    fun searchFulltext(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ArticlePageResponse> {
        if (query.trim().length < 2) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(articleService.searchFulltext(query, page, size))
    }

    @Operation(summary = "Get article detail by UUID")
    @GetMapping("/{externalId}")
    fun getArticleByExternalId(
        @PathVariable externalId: String,
    ): ResponseEntity<ArticleDetailResponse> = ResponseEntity.ok(articleService.getArticleByExternalId(externalId))

    @Operation(summary = "Track article view (records read history for authenticated users)")
    @PostMapping("/{articleId}/view")
    fun trackView(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<Void> {
        if (userDetails != null) {
            myPageService.trackArticleView(userDetails.id, articleId)
        }
        return ResponseEntity.ok().build()
    }
}
