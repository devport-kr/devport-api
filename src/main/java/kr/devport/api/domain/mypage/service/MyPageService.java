package kr.devport.api.domain.mypage.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.mypage.entity.UserReadHistory;
import kr.devport.api.domain.mypage.entity.UserSavedArticle;
import kr.devport.api.domain.mypage.dto.response.ReadHistoryResponse;
import kr.devport.api.domain.mypage.dto.response.SavedArticleResponse;
import kr.devport.api.domain.article.repository.ArticleRepository;
import kr.devport.api.domain.mypage.repository.UserReadHistoryRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.mypage.repository.UserSavedArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserSavedArticleRepository savedArticleRepository;
    private final UserReadHistoryRepository readHistoryRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    // --- Saved Articles ---

    public Page<SavedArticleResponse> getSavedArticles(Long userId, Pageable pageable) {
        return savedArticleRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(SavedArticleResponse::from);
    }

    @Transactional
    public void saveArticle(Long userId, String articleExternalId) {
        if (savedArticleRepository.existsByUserIdAndArticle_ExternalId(userId, articleExternalId)) {
            return; // Already saved, idempotent
        }

        Article article = articleRepository.findByExternalId(articleExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleExternalId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserSavedArticle saved = UserSavedArticle.builder()
            .user(user)
            .article(article)
            .build();
        savedArticleRepository.save(saved);
    }

    @Transactional
    public void unsaveArticle(Long userId, String articleExternalId) {
        savedArticleRepository.deleteByUserIdAndArticle_ExternalId(userId, articleExternalId);
    }

    public boolean isArticleSaved(Long userId, String articleExternalId) {
        return savedArticleRepository.existsByUserIdAndArticle_ExternalId(userId, articleExternalId);
    }

    // --- Read History ---

    public Page<ReadHistoryResponse> getReadHistory(Long userId, Pageable pageable) {
        return readHistoryRepository.findByUserIdOrderByReadAtDesc(userId, pageable)
            .map(ReadHistoryResponse::from);
    }

    @Transactional
    public void trackArticleView(Long userId, String articleExternalId) {
        Article article = articleRepository.findByExternalId(articleExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleExternalId));

        Optional<UserReadHistory> existing = readHistoryRepository
            .findByUserIdAndArticle_Id(userId, article.getId());

        if (existing.isPresent()) {
            // Update read timestamp
            existing.get().updateReadAt();
        } else {
            // Create new history entry
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            UserReadHistory history = UserReadHistory.builder()
                .user(user)
                .article(article)
                .build();
            readHistoryRepository.save(history);
        }
    }
}
