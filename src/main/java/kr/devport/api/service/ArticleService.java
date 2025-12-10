package kr.devport.api.service;

import kr.devport.api.domain.entity.Article;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.domain.enums.Source;
import kr.devport.api.dto.response.ArticleMetadataResponse;
import kr.devport.api.dto.response.ArticlePageResponse;
import kr.devport.api.dto.response.ArticleResponse;
import kr.devport.api.dto.response.TrendingTickerResponse;
import kr.devport.api.repository.ArticleRepository;
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
        value = "articles",
        key = "#category != null ? #category.name() + '_' + #page + '_' + #size : 'all_' + #page + '_' + #size"
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

        // 캐시에 DTO를 저장해 지연 로딩 문제를 피한다.
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


    @Cacheable(value = "trendingTicker", key = "#limit")
    public List<TrendingTickerResponse> getTrendingTicker(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<Article> articles = articleRepository.findAllByOrderByScoreDescCreatedAtSourceDesc(pageable);

        return articles.stream()
            .map(this::convertToTrendingTickerResponse)
            .collect(Collectors.toList());
    }

    private ArticleResponse convertToArticleResponse(Article article) {
        return ArticleResponse.builder()
            .id(article.getId())
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
}
