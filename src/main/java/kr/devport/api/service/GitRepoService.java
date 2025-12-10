package kr.devport.api.service;

import kr.devport.api.domain.entity.GitRepo;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.dto.response.GitRepoPageResponse;
import kr.devport.api.dto.response.GitRepoResponse;
import kr.devport.api.repository.GitRepoRepository;
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
        value = "gitRepos",
        key = "#category != null ? #category.name() + '_' + #page + '_' + #size : 'all_' + #page + '_' + #size"
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

    @Cacheable(value = "trendingGitRepos", key = "#page + '_' + #size")
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

    @Cacheable(value = "gitReposByLanguage", key = "#language + '_' + #limit")
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
