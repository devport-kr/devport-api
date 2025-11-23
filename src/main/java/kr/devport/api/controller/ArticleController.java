package kr.devport.api.controller;

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

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * GET /api/articles
     * Get articles with pagination and optional category filtering
     *
     * @param category Category filter (optional): ALL, AI_LLM, DEVOPS_SRE, BACKEND, INFRA_CLOUD, OTHER
     * @param page Page number (default: 0)
     * @param size Items per page (default: 9)
     * @return Paginated article response
     */
    @GetMapping
    public ResponseEntity<ArticlePageResponse> getArticles(
        @RequestParam(required = false) Category category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "9") int size
    ) {
        ArticlePageResponse response = articleService.getArticles(category, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/articles/github-trending
     * Get GitHub trending repositories
     *
     * @param limit Number of repos to return (default: 10)
     * @return List of GitHub article responses
     */
    @GetMapping("/github-trending")
    public ResponseEntity<List<ArticleResponse>> getGitHubTrending(
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<ArticleResponse> response = articleService.getGitHubTrending(limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/articles/trending-ticker
     * Get trending ticker articles
     *
     * @param limit Number of articles for ticker (default: 20)
     * @return List of trending ticker responses
     */
    @GetMapping("/trending-ticker")
    public ResponseEntity<List<TrendingTickerResponse>> getTrendingTicker(
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<TrendingTickerResponse> response = articleService.getTrendingTicker(limit);
        return ResponseEntity.ok(response);
    }
}
