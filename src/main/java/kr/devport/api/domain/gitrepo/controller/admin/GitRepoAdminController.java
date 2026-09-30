package kr.devport.api.domain.gitrepo.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.gitrepo.dto.request.admin.GitRepoCreateRequest;
import kr.devport.api.domain.gitrepo.dto.request.admin.GitRepoUpdateRequest;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoResponse;
import kr.devport.api.domain.gitrepo.service.admin.GitRepoAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/git-repos")
@RequiredArgsConstructor
public class GitRepoAdminController {

    private final GitRepoAdminService gitRepoAdminService;

    @PostMapping
    public ResponseEntity<GitRepoResponse> createGitRepo(@Valid @RequestBody GitRepoCreateRequest request) {
        GitRepoResponse response = gitRepoAdminService.createGitRepo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GitRepoResponse> updateGitRepo(
        @PathVariable Long id,
        @Valid @RequestBody GitRepoUpdateRequest request
    ) {
        GitRepoResponse response = gitRepoAdminService.updateGitRepo(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGitRepo(@PathVariable Long id) {
        gitRepoAdminService.deleteGitRepo(id);
        return ResponseEntity.noContent().build();
    }
}
