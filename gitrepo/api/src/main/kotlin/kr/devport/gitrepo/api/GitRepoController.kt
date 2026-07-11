package kr.devport.gitrepo.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.devport.gitrepo.Category
import kr.devport.gitrepo.service.GitRepoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "GitRepo", description = "GitHub Repository API")
@RestController
@RequestMapping("/api/git-repos")
class GitRepoController(
    private val gitRepoService: GitRepoService,
) {
    @Operation(summary = "Get git repos", description = "Paginated repositories with optional category filter")
    @GetMapping
    fun getGitRepos(
        @RequestParam(required = false) category: Category?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<GitRepoPageResponse> = ResponseEntity.ok(gitRepoService.getGitRepos(category, page, size).toResponse())

    @Operation(summary = "Get trending git repos", description = "Repositories sorted by stars gained this week")
    @GetMapping("/trending")
    fun getTrendingGitRepos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<GitRepoPageResponse> = ResponseEntity.ok(gitRepoService.getTrendingGitRepos(page, size).toResponse())

    @Operation(summary = "Get git repos by language", description = "Repositories filtered by programming language")
    @GetMapping("/language/{language}")
    fun getGitReposByLanguage(
        @PathVariable language: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ResponseEntity<List<GitRepoResponse>> =
        ResponseEntity.ok(gitRepoService.getGitReposByLanguage(language, limit).map { it.toResponse() })
}
