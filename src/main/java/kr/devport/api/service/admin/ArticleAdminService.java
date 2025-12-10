package kr.devport.api.service.admin;

import kr.devport.api.domain.entity.Article;
import kr.devport.api.domain.entity.ArticleMetadata;
import kr.devport.api.dto.request.admin.ArticleCreateRequest;
import kr.devport.api.dto.request.admin.ArticleUpdateRequest;
import kr.devport.api.dto.response.ArticleMetadataResponse;
import kr.devport.api.dto.response.ArticleResponse;
import kr.devport.api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleAdminService {  

    private final ArticleRepository articleRepository;

    @CacheEvict(value = {"articles", "trendingTicker"}, allEntries = true)
    public ArticleResponse createArticle(ArticleCreateRequest request) {
        Article article = Article.builder()
            .itemType(request.getItemType())
            .source(request.getSource())
            .category(request.getCategory())
            .summaryKoTitle(request.getSummaryKoTitle())
            .summaryKoBody(request.getSummaryKoBody())
            .titleEn(request.getTitleEn())
            .url(request.getUrl())
            .score(request.getScore())
            .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
            .createdAtSource(request.getCreatedAtSource())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        if (request.getMetadata() != null) {
            article.setMetadata(ArticleMetadata.builder()
                .stars(request.getMetadata().getStars())
                .comments(request.getMetadata().getComments())
                .upvotes(request.getMetadata().getUpvotes())
                .readTime(request.getMetadata().getReadTime())
                .language(request.getMetadata().getLanguage())
                .build());
        }

        Article saved = articleRepository.save(article);
        return convertToResponse(saved);
    }

    @CacheEvict(value = {"articles", "trendingTicker"}, allEntries = true)
    public ArticleResponse updateArticle(Long id, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Article not found with id: " + id));

        if (request.getItemType() != null) article.setItemType(request.getItemType());
        if (request.getSource() != null) article.setSource(request.getSource());
        if (request.getCategory() != null) article.setCategory(request.getCategory());
        if (request.getSummaryKoTitle() != null) article.setSummaryKoTitle(request.getSummaryKoTitle());
        if (request.getSummaryKoBody() != null) article.setSummaryKoBody(request.getSummaryKoBody());
        if (request.getTitleEn() != null) article.setTitleEn(request.getTitleEn());
        if (request.getUrl() != null) article.setUrl(request.getUrl());
        if (request.getScore() != null) article.setScore(request.getScore());
        if (request.getTags() != null) article.setTags(new ArrayList<>(request.getTags()));
        if (request.getCreatedAtSource() != null) article.setCreatedAtSource(request.getCreatedAtSource());

        if (request.getMetadata() != null) {
            ArticleMetadata metadata = article.getMetadata();
            if (metadata == null) {
                metadata = new ArticleMetadata();
                article.setMetadata(metadata);
            }
            if (request.getMetadata().getStars() != null) metadata.setStars(request.getMetadata().getStars());
            if (request.getMetadata().getComments() != null) metadata.setComments(request.getMetadata().getComments());
            if (request.getMetadata().getUpvotes() != null) metadata.setUpvotes(request.getMetadata().getUpvotes());
            if (request.getMetadata().getReadTime() != null) metadata.setReadTime(request.getMetadata().getReadTime());
            if (request.getMetadata().getLanguage() != null) metadata.setLanguage(request.getMetadata().getLanguage());
        }

        article.setUpdatedAt(LocalDateTime.now());
        Article updated = articleRepository.save(article);
        return convertToResponse(updated);
    }

    @CacheEvict(value = {"articles", "trendingTicker"}, allEntries = true)
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new IllegalArgumentException("Article not found with id: " + id);
        }
        articleRepository.deleteById(id);
    }

    private ArticleResponse convertToResponse(Article article) {
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
            .tags(article.getTags() != null ? new ArrayList<>(article.getTags()) : new ArrayList<>())
            .createdAtSource(article.getCreatedAtSource())
            .metadata(convertToMetadataResponse(article.getMetadata()))
            .build();
    }

    private ArticleMetadataResponse convertToMetadataResponse(ArticleMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return ArticleMetadataResponse.builder()
            .stars(metadata.getStars())
            .comments(metadata.getComments())
            .upvotes(metadata.getUpvotes())
            .readTime(metadata.getReadTime())
            .language(metadata.getLanguage())
            .build();
    }
}
