package kr.devport.api.domain.mypage.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.mypage.dto.response.ReadHistoryResponse
import kr.devport.api.domain.mypage.dto.response.SavedArticleResponse
import kr.devport.api.domain.mypage.service.MyPageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "My Page", description = "User's personal page endpoints")
@RestController
@RequestMapping("/api/me")
class MyPageController(
    private val myPageService: MyPageService,
) {
    @Operation(summary = "Get saved articles")
    @GetMapping("/saved-articles")
    fun getSavedArticles(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<Page<SavedArticleResponse>> = ResponseEntity.ok(myPageService.getSavedArticles(userDetails.id, pageable))

    @Operation(summary = "Save an article")
    @PostMapping("/saved-articles/{articleId}")
    fun saveArticle(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Void> {
        myPageService.saveArticle(userDetails.id, articleId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(summary = "Remove article from saved")
    @DeleteMapping("/saved-articles/{articleId}")
    fun unsaveArticle(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Void> {
        myPageService.unsaveArticle(userDetails.id, articleId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Check if article is saved")
    @GetMapping("/saved-articles/{articleId}/status")
    fun isArticleSaved(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Map<String, Boolean>> = ResponseEntity.ok(mapOf("saved" to myPageService.isArticleSaved(userDetails.id, articleId)))

    @Operation(summary = "Get read history")
    @GetMapping("/read-history")
    fun getReadHistory(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PageableDefault(size = 20, sort = ["readAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ResponseEntity<Page<ReadHistoryResponse>> = ResponseEntity.ok(myPageService.getReadHistory(userDetails.id, pageable))
}
