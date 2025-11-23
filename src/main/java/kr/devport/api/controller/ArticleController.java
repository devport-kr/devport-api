package kr.devport.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.dto.response.ArticlePageResponse;
import kr.devport.api.dto.response.ArticleResponse;
import kr.devport.api.dto.response.TrendingTickerResponse;
import kr.devport.api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Articles", description = "Article and content management endpoints (public)")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(
        summary = "Get articles with pagination",
        description = "Retrieve articles with optional category filtering and pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved articles",
            content = @Content(schema = @Schema(implementation = ArticlePageResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<ArticlePageResponse> getArticles(
        @Parameter(description = "Category filter: ALL, AI_LLM, DEVOPS_SRE, BACKEND, INFRA_CLOUD, OTHER")
        @RequestParam(required = false) Category category,
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of items per page")
        @RequestParam(defaultValue = "9") int size
    ) {
        ArticlePageResponse response = articleService.getArticles(category, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get GitHub trending repositories",
        description = "Retrieve currently trending repositories from GitHub"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved GitHub trending repositories"
        )
    })
    @GetMapping("/github-trending")
    public ResponseEntity<List<ArticleResponse>> getGitHubTrending(
        @Parameter(description = "Number of trending repos to return")
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<ArticleResponse> response = articleService.getGitHubTrending(limit);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get trending ticker articles",
        description = "Retrieve trending articles for the ticker display"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved trending ticker articles"
        )
    })
    @GetMapping("/trending-ticker")
    public ResponseEntity<List<TrendingTickerResponse>> getTrendingTicker(
        @Parameter(description = "Number of articles for ticker display")
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<TrendingTickerResponse> response = articleService.getTrendingTicker(limit);
        return ResponseEntity.ok(response);
    }
}
