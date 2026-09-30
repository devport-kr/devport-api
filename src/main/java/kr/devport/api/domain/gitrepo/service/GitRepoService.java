package kr.devport.api.domain.gitrepo.service;

import kr.devport.api.domain.gitrepo.entity.GitRepo;
import kr.devport.api.domain.gitrepo.enums.Category;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoPageResponse;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoResponse;
import kr.devport.api.domain.gitrepo.repository.GitRepoRepository;
import kr.devport.api.domain.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GitRepoService {

    private final GitRepoRepository gitRepoRepository;

    @Cacheable(
        value = CacheNames.GIT_REPOS,
        key = "@cacheKeyFactory.gitRepoListKey(#category, #page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')"
    )
    public GitRepoPageResponse getGitRepos(Category category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "score")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<GitRepo> gitRepoPage;

        if (category == null) {
            gitRepoPage = gitRepoRepository.findAll(pageable);
        } else {
            gitRepoPage = gitRepoRepository.findByCategory(category, pageable);
        }

        // 캐시에 DTO를 넣어 지연 로딩 문제를 피한다.
        return GitRepoPageResponse.builder()
            .content(gitRepoPage.getContent().stream()
                .map(this::convertToGitRepoResponse)
                .collect(Collectors.toList()))
            .totalElements(gitRepoPage.getTotalElements())
            .totalPages(gitRepoPage.getTotalPages())
            .currentPage(gitRepoPage.getNumber())
            .hasMore(gitRepoPage.hasNext())
            .build();
    }

    @Cacheable(
        value = CacheNames.TRENDING_GIT_REPOS,
        key = "@cacheKeyFactory.trendingGitReposKey(#page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')"
    )
    public GitRepoPageResponse getTrendingGitRepos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<GitRepo> gitRepoPage = gitRepoRepository.findAllByOrderByStarsThisWeekDesc(pageable);

        return GitRepoPageResponse.builder()
            .content(gitRepoPage.getContent().stream()
                .map(this::convertToGitRepoResponse)
                .collect(Collectors.toList()))
            .totalElements(gitRepoPage.getTotalElements())
            .totalPages(gitRepoPage.getTotalPages())
            .currentPage(gitRepoPage.getNumber())
            .hasMore(gitRepoPage.hasNext())
            .build();
    }

    @Cacheable(
        value = CacheNames.GIT_REPOS_BY_LANGUAGE,
        key = "@cacheKeyFactory.gitReposByLanguageKey(#language, #limit)",
        unless = "@cacheFallbackBypass.shouldBypass('GIT_REPO')"
    )
    public List<GitRepoResponse> getGitReposByLanguage(String language, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        Page<GitRepo> gitRepos = gitRepoRepository.findByLanguageOrderByScoreDesc(language, pageable);

        return gitRepos.getContent().stream()
            .map(this::convertToGitRepoResponse)
            .collect(Collectors.toList());
    }

    private GitRepoResponse convertToGitRepoResponse(GitRepo gitRepo) {
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
            .createdAt(gitRepo.getCreatedAt())
            .updatedAt(gitRepo.getUpdatedAt())
            .build();
    }
}
