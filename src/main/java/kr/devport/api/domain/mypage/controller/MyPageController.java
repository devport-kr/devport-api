package kr.devport.api.domain.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.mypage.dto.response.ReadHistoryResponse;
import kr.devport.api.domain.mypage.dto.response.SavedArticleResponse;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "My Page", description = "User's personal page endpoints")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "Get saved articles", description = "Retrieve user's saved/bookmarked articles with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved saved articles"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/saved-articles")
    public ResponseEntity<Page<SavedArticleResponse>> getSavedArticles(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SavedArticleResponse> saved = myPageService.getSavedArticles(userDetails.getId(), pageable);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Save an article", description = "Add an article to user's saved/bookmarked list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Article saved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "Article not found")
    })
    @PostMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> saveArticle(
        @Parameter(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        @PathVariable String articleId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        myPageService.saveArticle(userDetails.getId(), articleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove article from saved", description = "Remove an article from user's saved/bookmarked list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Article removed from saved list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @DeleteMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> unsaveArticle(
        @Parameter(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        @PathVariable String articleId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        myPageService.unsaveArticle(userDetails.getId(), articleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if article is saved", description = "Check if an article is in user's saved/bookmarked list")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(schema = @Schema(example = "{\"saved\": true}"))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/saved-articles/{articleId}/status")
    public ResponseEntity<Map<String, Boolean>> isArticleSaved(
        @Parameter(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        @PathVariable String articleId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean saved = myPageService.isArticleSaved(userDetails.getId(), articleId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    @Operation(summary = "Get read history", description = "Retrieve user's article read history with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved read history"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/read-history")
    public ResponseEntity<Page<ReadHistoryResponse>> getReadHistory(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 20, sort = "readAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReadHistoryResponse> history = myPageService.getReadHistory(userDetails.getId(), pageable);
        return ResponseEntity.ok(history);
    }
}
