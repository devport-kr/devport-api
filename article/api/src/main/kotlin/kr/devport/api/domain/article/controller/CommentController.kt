package kr.devport.api.domain.article.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.devport.api.domain.article.dto.request.CommentCreateRequest
import kr.devport.api.domain.article.dto.request.CommentUpdateRequest
import kr.devport.api.domain.article.dto.response.CommentResponse
import kr.devport.api.domain.article.service.CommentService
import kr.devport.api.domain.common.security.CustomUserDetails
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Comments", description = "Article comment endpoints")
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
class CommentController(
    private val commentService: CommentService,
) {
    @Operation(summary = "Get comments for an article")
    @GetMapping
    fun getComments(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<List<CommentResponse>> = ResponseEntity.ok(commentService.getCommentsByArticle(articleId, userDetails?.id))

    @Operation(summary = "Create a comment (or reply)")
    @PostMapping
    fun createComment(
        @PathVariable articleId: String,
        @Valid @RequestBody request: CommentCreateRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<CommentResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(commentService.createComment(articleId, request, userDetails.id))

    @Operation(summary = "Update own comment")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable articleId: String,
        @PathVariable commentId: String,
        @Valid @RequestBody request: CommentUpdateRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<CommentResponse> = ResponseEntity.ok(commentService.updateComment(commentId, request, userDetails.id))

    @Operation(summary = "Delete own comment (soft delete)")
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable articleId: String,
        @PathVariable commentId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Void> {
        commentService.deleteComment(commentId, userDetails.id)
        return ResponseEntity.noContent().build()
    }
}
