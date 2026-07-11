package kr.devport.gitrepo.api

import jakarta.validation.Valid
import kr.devport.gitrepo.service.GitRepoAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/git-repos")
class GitRepoAdminController(
    private val gitRepoAdminService: GitRepoAdminService,
) {
    @PostMapping
    fun createGitRepo(
        @Valid @RequestBody request: GitRepoCreateRequest,
    ): ResponseEntity<GitRepoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(gitRepoAdminService.create(request.toCommand()).toResponse())

    @PutMapping("/{id}")
    fun updateGitRepo(
        @PathVariable id: Long,
        @Valid @RequestBody request: GitRepoUpdateRequest,
    ): ResponseEntity<GitRepoResponse> = ResponseEntity.ok(gitRepoAdminService.update(id, request.toCommand()).toResponse())

    @DeleteMapping("/{id}")
    fun deleteGitRepo(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        gitRepoAdminService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
