package kr.devport.api.domain.gitrepo.service.admin;

import kr.devport.api.domain.gitrepo.entity.GitRepo;
import kr.devport.api.domain.gitrepo.dto.request.admin.GitRepoCreateRequest;
import kr.devport.api.domain.gitrepo.dto.request.admin.GitRepoUpdateRequest;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoResponse;
import kr.devport.api.domain.gitrepo.repository.GitRepoRepository;
import lombok.RequiredArgsConstructor;
import kr.devport.api.domain.common.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class GitRepoAdminService {

    private final GitRepoRepository gitRepoRepository;

    @CacheEvict(value = {
        CacheNames.GIT_REPOS,
        CacheNames.TRENDING_GIT_REPOS,
        CacheNames.GIT_REPOS_BY_LANGUAGE
    }, allEntries = true)
    public GitRepoResponse createGitRepo(GitRepoCreateRequest request) {
        GitRepo gitRepo = GitRepo.builder()
            .fullName(request.getFullName())
            .url(request.getUrl())
            .description(request.getDescription())
            .language(request.getLanguage())
            .stars(request.getStars())
            .forks(request.getForks())
            .starsThisWeek(request.getStarsThisWeek())
            .summaryKoTitle(request.getSummaryKoTitle())
            .summaryKoBody(request.getSummaryKoBody())
            .category(request.getCategory())
            .score(request.getScore())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        GitRepo saved = gitRepoRepository.save(gitRepo);
        return convertToResponse(saved);
    }

    @CacheEvict(value = {
        CacheNames.GIT_REPOS,
        CacheNames.TRENDING_GIT_REPOS,
        CacheNames.GIT_REPOS_BY_LANGUAGE
    }, allEntries = true)
    public GitRepoResponse updateGitRepo(Long id, GitRepoUpdateRequest request) {
        GitRepo gitRepo = gitRepoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("GitRepo not found with id: " + id));

        if (request.getFullName() != null) gitRepo.setFullName(request.getFullName());
        if (request.getUrl() != null) gitRepo.setUrl(request.getUrl());
        if (request.getDescription() != null) gitRepo.setDescription(request.getDescription());
        if (request.getLanguage() != null) gitRepo.setLanguage(request.getLanguage());
        if (request.getStars() != null) gitRepo.setStars(request.getStars());
        if (request.getForks() != null) gitRepo.setForks(request.getForks());
        if (request.getStarsThisWeek() != null) gitRepo.setStarsThisWeek(request.getStarsThisWeek());
        if (request.getSummaryKoTitle() != null) gitRepo.setSummaryKoTitle(request.getSummaryKoTitle());
        if (request.getSummaryKoBody() != null) gitRepo.setSummaryKoBody(request.getSummaryKoBody());
        if (request.getCategory() != null) gitRepo.setCategory(request.getCategory());
        if (request.getScore() != null) gitRepo.setScore(request.getScore());

        gitRepo.setUpdatedAt(LocalDateTime.now());
        GitRepo updated = gitRepoRepository.save(gitRepo);
        return convertToResponse(updated);
    }

    @CacheEvict(value = {
        CacheNames.GIT_REPOS,
        CacheNames.TRENDING_GIT_REPOS,
        CacheNames.GIT_REPOS_BY_LANGUAGE
    }, allEntries = true)
    public void deleteGitRepo(Long id) {
        if (!gitRepoRepository.existsById(id)) {
            throw new IllegalArgumentException("GitRepo not found with id: " + id);
        }
        gitRepoRepository.deleteById(id);
    }

    private GitRepoResponse convertToResponse(GitRepo gitRepo) {
        return GitRepoResponse.builder()
            .id(gitRepo.getId())
            .fullName(gitRepo.getFullName())
            .url(gitRepo.getUrl())
            .description(gitRepo.getDescription())
            .language(gitRepo.getLanguage())
            .stars(gitRepo.getStars())
            .forks(gitRepo.getForks())
            .starsThisWeek(gitRepo.getStarsThisWeek())
            .summaryKoTitle(gitRepo.getSummaryKoTitle())
            .summaryKoBody(gitRepo.getSummaryKoBody())
            .category(gitRepo.getCategory())
            .score(gitRepo.getScore())
            .build();
    }
}
