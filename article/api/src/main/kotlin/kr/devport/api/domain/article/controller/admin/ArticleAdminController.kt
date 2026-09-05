package kr.devport.api.domain.article.controller.admin

import jakarta.validation.Valid
import kr.devport.api.domain.article.dto.request.admin.ArticleCreateRequest
import kr.devport.api.domain.article.dto.request.admin.ArticleLLMCreateRequest
import kr.devport.api.domain.article.dto.request.admin.ArticleUpdateRequest
import kr.devport.api.domain.article.dto.response.ArticleLLMPreviewResponse
import kr.devport.api.domain.article.dto.response.ArticlePageResponse
import kr.devport.api.domain.article.dto.response.ArticleResponse
import kr.devport.api.domain.article.service.admin.ArticleAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/articles")
class ArticleAdminController(
    private val articleAdminService: ArticleAdminService,
) {
    @PostMapping
    fun createArticle(
        @Valid @RequestBody request: ArticleCreateRequest,
    ): ResponseEntity<ArticleResponse> = ResponseEntity.status(HttpStatus.CREATED).body(articleAdminService.createArticle(request))

    @PostMapping("/llm-process")
    fun processArticleWithLLM(
        @Valid @RequestBody request: ArticleLLMCreateRequest,
    ): ResponseEntity<ArticleResponse> = ResponseEntity.status(HttpStatus.CREATED).body(articleAdminService.createArticleFromLLM(request))

    @PostMapping("/llm-preview")
    fun previewArticleLLM(
        @Valid @RequestBody request: ArticleLLMCreateRequest,
    ): ResponseEntity<ArticleLLMPreviewResponse> = ResponseEntity.ok(articleAdminService.previewArticleLLM(request))

    @GetMapping
    fun listArticles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ArticlePageResponse> = ResponseEntity.ok(articleAdminService.listArticles(page, size, search))

    @PutMapping("/{id}")
    fun updateArticle(
        @PathVariable id: Long,
        @Valid @RequestBody request: ArticleUpdateRequest,
    ): ResponseEntity<ArticleResponse> = ResponseEntity.ok(articleAdminService.updateArticle(id, request))

    @DeleteMapping("/{id}")
    fun deleteArticle(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        articleAdminService.deleteArticle(id)
        return ResponseEntity.noContent().build()
    }
}
