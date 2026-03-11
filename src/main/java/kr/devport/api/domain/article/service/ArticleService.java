package kr.devport.api.domain.article.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.dto.request.ArticleSearchCondition;
import kr.devport.api.domain.article.dto.response.ArticleAutocompleteListResponse;
import kr.devport.api.domain.article.dto.response.ArticleAutocompleteResponse;
import kr.devport.api.domain.article.dto.response.ArticleDetailResponse;
import kr.devport.api.domain.article.dto.response.ArticleMetadataResponse;
import kr.devport.api.domain.article.dto.response.ArticlePageResponse;
import kr.devport.api.domain.article.dto.response.ArticleResponse;
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse;
import kr.devport.api.domain.article.repository.ArticleRepository;
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
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Cacheable(
        value = CacheNames.ARTICLES,
        key = "@cacheKeyFactory.articleListKey(#category, #page, #size)",
        unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')"
    )
    public ArticlePageResponse getArticles(Category category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "score")
                .and(Sort.by(Sort.Direction.DESC, "createdAtSource"))
        );

        Page<Article> articlePage;

        if (category == null) {
            articlePage = articleRepository.findAll(pageable);
        } else {
            articlePage = articleRepository.findByCategory(category, pageable);
        }


        return ArticlePageResponse.builder()
            .content(articlePage.getContent().stream()
                .map(this::convertToArticleResponse)
                .collect(Collectors.toList()))
            .totalElements(articlePage.getTotalElements())
            .totalPages(articlePage.getTotalPages())
            .currentPage(articlePage.getNumber())
            .hasMore(articlePage.hasNext())
            .build();
    }


    @Cacheable(
        value = CacheNames.TRENDING_TICKER,
        key = "@cacheKeyFactory.trendingTickerKey(#limit)",
        unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')"
    )
    public List<TrendingTickerResponse> getTrendingTicker(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<Article> articles = articleRepository.findAllByOrderByScoreDescCreatedAtSourceDesc(pageable);

        return articles.stream()
            .map(this::convertToTrendingTickerResponse)
            .collect(Collectors.toList());
    }

    /**
     * QueryDSL을 사용한 동적 검색
     * - 8개 선택적 필터 조건을 타입 안전하게 처리
     * - BooleanExpression 조합으로 null 조건 자동 제외
     */
    public ArticlePageResponse searchArticles(ArticleSearchCondition condition, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Article> articlePage = articleRepository.searchWithCondition(condition, pageable);

        return ArticlePageResponse.builder()
            .content(articlePage.getContent().stream()
                .map(this::convertToArticleResponse)
                .collect(Collectors.toList()))
            .totalElements(articlePage.getTotalElements())
            .totalPages(articlePage.getTotalPages())
            .currentPage(articlePage.getNumber())
            .hasMore(articlePage.hasNext())
            .build();
    }

    public ArticleDetailResponse getArticleByExternalId(String externalId) {
        Article article = articleRepository.findByExternalId(externalId)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + externalId));
        return convertToArticleDetailResponse(article);
    }

    private ArticleDetailResponse convertToArticleDetailResponse(Article article) {
        return ArticleDetailResponse.builder()
            .externalId(article.getExternalId())
            .itemType(article.getItemType())
            .source(article.getSource())
            .category(article.getCategory())
            .summaryKoTitle(article.getSummaryKoTitle())
            .summaryKoBody(article.getSummaryKoBody())
            .titleEn(article.getTitleEn())
            .url(article.getUrl())
            .score(article.getScore())
            .tags(article.getTags() != null ? new java.util.ArrayList<>(article.getTags()) : new java.util.ArrayList<>())
            .createdAtSource(article.getCreatedAtSource())
            .metadata(convertToMetadataResponse(article))
            .build();
    }

    private ArticleResponse convertToArticleResponse(Article article) {
        return ArticleResponse.builder()
            .id(article.getId())
            .externalId(article.getExternalId())
            .itemType(article.getItemType())
            .source(article.getSource())
            .category(article.getCategory())
            .summaryKoTitle(article.getSummaryKoTitle())
            .summaryKoBody(article.getSummaryKoBody())
            .titleEn(article.getTitleEn())
            .url(article.getUrl())
            .score(article.getScore())
            .tags(article.getTags() != null ? new java.util.ArrayList<>(article.getTags()) : new java.util.ArrayList<>())
            .createdAtSource(article.getCreatedAtSource())
            .metadata(convertToMetadataResponse(article))
            .build();
    }

    private TrendingTickerResponse convertToTrendingTickerResponse(Article article) {
        return TrendingTickerResponse.builder()
            .id(article.getId())
            .summaryKoTitle(article.getSummaryKoTitle())
            .url(article.getUrl())
            .createdAtSource(article.getCreatedAtSource())
            .build();
    }

    private ArticleMetadataResponse convertToMetadataResponse(Article article) {
        if (article.getMetadata() == null) {
            return null;
        }

        return ArticleMetadataResponse.builder()
            .stars(article.getMetadata().getStars())
            .comments(article.getMetadata().getComments())
            .upvotes(article.getMetadata().getUpvotes())
            .readTime(article.getMetadata().getReadTime())
            .language(article.getMetadata().getLanguage())
            .build();
    }

    // ========== Autocomplete/Fulltext Search Methods ==========

    /**
     * 자동완성 검색 - 상위 5개 결과 반환
     * - 제목 매칭 우선
     * - 최신순 2차 정렬
     * - 총 검색 결과 수 포함 ("전체 X개 결과 보기" UI용)
     */
    public ArticleAutocompleteListResponse searchAutocomplete(String query) {
        List<Article> articles = articleRepository.searchAutocomplete(query, 5);
        Long totalMatches = articleRepository.countFulltextMatches(query);

        List<ArticleAutocompleteResponse> suggestions = articles.stream()
            .map(article -> convertToAutocompleteResponse(article, query))
            .collect(Collectors.toList());

        return ArticleAutocompleteListResponse.builder()
            .suggestions(suggestions)
            .totalMatches(totalMatches != null ? totalMatches : 0L)
            .build();
    }

    /**
     * 전체 텍스트 검색 - 페이지네이션 결과 반환
     * - 제목 매칭 우선
     * - 최신순 2차 정렬
     */
    public ArticlePageResponse searchFulltext(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Article> articlePage = articleRepository.searchFulltext(query, pageable);

        return ArticlePageResponse.builder()
            .content(articlePage.getContent().stream()
                .map(this::convertToArticleResponse)
                .collect(Collectors.toList()))
            .totalElements(articlePage.getTotalElements())
            .totalPages(articlePage.getTotalPages())
            .currentPage(articlePage.getNumber())
            .hasMore(articlePage.hasNext())
            .build();
    }

    private ArticleAutocompleteResponse convertToAutocompleteResponse(Article article, String query) {
        String searchTerm = query.trim().toLowerCase();
        ArticleAutocompleteResponse.MatchType matchType =
            article.getSummaryKoTitle() != null &&
            article.getSummaryKoTitle().toLowerCase().contains(searchTerm)
                ? ArticleAutocompleteResponse.MatchType.TITLE
                : ArticleAutocompleteResponse.MatchType.BODY;

        return ArticleAutocompleteResponse.builder()
            .externalId(article.getExternalId())
            .summaryKoTitle(article.getSummaryKoTitle())
            .source(article.getSource())
            .category(article.getCategory() != null ? article.getCategory().name() : null)
            .matchType(matchType)
            .score(article.getScore())
            .build();
    }
}
