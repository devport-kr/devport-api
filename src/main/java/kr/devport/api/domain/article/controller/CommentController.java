package kr.devport.api.domain.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.devport.api.domain.article.dto.request.CommentCreateRequest;
import kr.devport.api.domain.article.dto.request.CommentUpdateRequest;
import kr.devport.api.domain.article.dto.response.CommentResponse;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.article.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Comments", description = "Article comment endpoints")
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Get comments for an article")
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
        @PathVariable String articleId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        List<CommentResponse> comments = commentService.getCommentsByArticle(articleId, userId);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Create a comment (or reply)")
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable String articleId,
        @Valid @RequestBody CommentCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CommentResponse response = commentService.createComment(articleId, request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update own comment")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
        @PathVariable String articleId,
        @PathVariable String commentId,
        @Valid @RequestBody CommentUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CommentResponse response = commentService.updateComment(commentId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete own comment (soft delete)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable String articleId,
        @PathVariable String commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
