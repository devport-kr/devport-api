package kr.devport.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.dto.response.GitRepoPageResponse;
import kr.devport.api.dto.response.GitRepoResponse;
import kr.devport.api.service.GitRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "GitRepo", description = "GitHub Repository API")
@RestController
@RequestMapping("/api/git-repos")
@RequiredArgsConstructor
public class GitRepoController {

    private final GitRepoService gitRepoService;

    @Operation(
        summary = "Get git repos",
        description = "Get paginated list of GitHub repositories with optional category filtering"
    )
    @GetMapping
    public ResponseEntity<GitRepoPageResponse> getGitRepos(
        @Parameter(description = "Category filter (optional)")
        @RequestParam(required = false) Category category,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "10") int size
    ) {
        GitRepoPageResponse response = gitRepoService.getGitRepos(category, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get trending git repos",
        description = "Get paginated list of trending GitHub repositories sorted by starsThisWeek"
    )
    @GetMapping("/trending")
    public ResponseEntity<GitRepoPageResponse> getTrendingGitRepos(
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "10") int size
    ) {
        GitRepoPageResponse response = gitRepoService.getTrendingGitRepos(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get git repos by language",
        description = "Get repositories filtered by programming language"
    )
    @GetMapping("/language/{language}")
    public ResponseEntity<List<GitRepoResponse>> getGitReposByLanguage(
        @Parameter(description = "Programming language (e.g., JavaScript, Python, Rust)")
        @PathVariable String language,

        @Parameter(description = "Number of repositories to return")
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<GitRepoResponse> response = gitRepoService.getGitReposByLanguage(language, limit);
        return ResponseEntity.ok(response);
    }
}
