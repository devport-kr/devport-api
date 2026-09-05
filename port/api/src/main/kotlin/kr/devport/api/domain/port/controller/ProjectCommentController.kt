package kr.devport.api.domain.port.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.port.dto.request.CommentCreateRequest
import kr.devport.api.domain.port.dto.request.CommentUpdateRequest
import kr.devport.api.domain.port.dto.request.VoteRequest
import kr.devport.api.domain.port.dto.response.ProjectCommentResponse
import kr.devport.api.domain.port.dto.response.VoteResponse
import kr.devport.api.domain.port.service.ProjectCommentService
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

@Tag(name = "Project Comments", description = "Project discussion endpoints")
@RestController
@RequestMapping("/api/projects/{projectId}/comments")
class ProjectCommentController(
    private val commentService: ProjectCommentService,
) {
    @Operation(
        summary = "Get project comments",
        description = "Retrieve all comments for a project (Reddit-style threading)",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Comments retrieved successfully",
                content = [Content(schema = Schema(implementation = ProjectCommentResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Project not found", content = [Content()]),
        ],
    )
    @GetMapping
    fun getComments(
        @PathVariable projectId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<List<ProjectCommentResponse>> {
        val userId = userDetails?.id
        return ResponseEntity.ok(commentService.getCommentsByProject(projectId, userId))
    }

    @Operation(summary = "Create comment", description = "Post a new comment or reply to an existing comment")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Comment created successfully",
                content = [Content(schema = Schema(implementation = ProjectCommentResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request", content = [Content()]),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Project or parent comment not found", content = [Content()]),
        ],
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    fun createComment(
        @PathVariable projectId: String,
        @Valid @RequestBody request: CommentCreateRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<ProjectCommentResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(commentService.createComment(projectId, request, userDetails.id))

    @Operation(summary = "Update comment", description = "Edit your own comment")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Comment updated successfully",
                content = [Content(schema = Schema(implementation = ProjectCommentResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request or not comment owner", content = [Content()]),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Comment not found", content = [Content()]),
        ],
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable projectId: String,
        @PathVariable commentId: String,
        @Valid @RequestBody request: CommentUpdateRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<ProjectCommentResponse> = ResponseEntity.ok(commentService.updateComment(commentId, request, userDetails.id))

    @Operation(summary = "Delete comment", description = "Soft delete your own comment")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            ApiResponse(responseCode = "400", description = "Not comment owner", content = [Content()]),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Comment not found", content = [Content()]),
        ],
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable projectId: String,
        @PathVariable commentId: String,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<Void> {
        commentService.deleteComment(commentId, userDetails.id)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Vote on comment",
        description = "Upvote (+1), downvote (-1), or remove vote (0) on a comment",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vote recorded successfully",
                content = [Content(schema = Schema(implementation = VoteResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid vote value", content = [Content()]),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Comment not found", content = [Content()]),
        ],
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{commentId}/vote")
    fun voteOnComment(
        @PathVariable projectId: String,
        @PathVariable commentId: String,
        @Valid @RequestBody request: VoteRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ResponseEntity<VoteResponse> = ResponseEntity.ok(commentService.voteOnComment(commentId, request, userDetails.id))
}
