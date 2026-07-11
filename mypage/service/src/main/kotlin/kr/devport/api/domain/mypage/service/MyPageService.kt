package kr.devport.api.domain.mypage.service

import kr.devport.api.domain.article.infrastructure.ArticleDirectory
import kr.devport.api.domain.mypage.dto.response.ReadHistoryResponse
import kr.devport.api.domain.mypage.dto.response.SavedArticleResponse
import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import kr.devport.api.domain.mypage.infrastructure.UserReadHistoryRepository
import kr.devport.api.domain.mypage.infrastructure.UserSavedArticleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MyPageService(
    private val savedArticleRepository: UserSavedArticleRepository,
    private val readHistoryRepository: UserReadHistoryRepository,
    private val articleDirectory: ArticleDirectory,
) {
    // --- Saved Articles ---

    fun getSavedArticles(
        userId: Long,
        pageable: Pageable,
    ): Page<SavedArticleResponse> {
        val page = savedArticleRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        val articles = articleDirectory.findByIds(page.content.map { it.articleId }.toSet())
        return page.map { SavedArticleResponse.from(it, articles[it.articleId]) }
    }

    @Transactional
    fun saveArticle(
        userId: Long,
        articleExternalId: String,
    ) {
        val article =
            articleDirectory.findByExternalId(articleExternalId)
                ?: throw IllegalArgumentException("Article not found: $articleExternalId")

        if (savedArticleRepository.existsByUserIdAndArticleId(userId, article.id)) {
            return // Already saved, idempotent
        }

        savedArticleRepository.save(
            UserSavedArticle().apply {
                this.userId = userId
                this.articleId = article.id
            },
        )
    }

    @Transactional
    fun unsaveArticle(
        userId: Long,
        articleExternalId: String,
    ) {
        val article = articleDirectory.findByExternalId(articleExternalId) ?: return
        savedArticleRepository.deleteByUserIdAndArticleId(userId, article.id)
    }

    fun isArticleSaved(
        userId: Long,
        articleExternalId: String,
    ): Boolean {
        val article = articleDirectory.findByExternalId(articleExternalId) ?: return false
        return savedArticleRepository.existsByUserIdAndArticleId(userId, article.id)
    }

    // --- Read History ---

    fun getReadHistory(
        userId: Long,
        pageable: Pageable,
    ): Page<ReadHistoryResponse> {
        val page = readHistoryRepository.findByUserIdOrderByReadAtDesc(userId, pageable)
        val articles = articleDirectory.findByIds(page.content.map { it.articleId }.toSet())
        return page.map { ReadHistoryResponse.from(it, articles[it.articleId]) }
    }

    @Transactional
    fun trackArticleView(
        userId: Long,
        articleExternalId: String,
    ) {
        val article =
            articleDirectory.findByExternalId(articleExternalId)
                ?: throw IllegalArgumentException("Article not found: $articleExternalId")

        val existing = readHistoryRepository.findByUserIdAndArticleId(userId, article.id)
        if (existing != null) {
            existing.updateReadAt()
        } else {
            readHistoryRepository.save(
                UserReadHistory().apply {
                    this.userId = userId
                    this.articleId = article.id
                },
            )
        }
    }
}
