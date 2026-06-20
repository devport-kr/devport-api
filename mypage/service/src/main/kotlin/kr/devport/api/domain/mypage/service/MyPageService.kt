package kr.devport.api.domain.mypage.service

import kr.devport.api.domain.article.repository.ArticleRepository
import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.mypage.dto.response.ReadHistoryResponse
import kr.devport.api.domain.mypage.dto.response.SavedArticleResponse
import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import kr.devport.api.domain.mypage.repository.UserReadHistoryRepository
import kr.devport.api.domain.mypage.repository.UserSavedArticleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MyPageService(
    private val savedArticleRepository: UserSavedArticleRepository,
    private val readHistoryRepository: UserReadHistoryRepository,
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository,
) {
    // --- Saved Articles ---

    fun getSavedArticles(
        userId: Long,
        pageable: Pageable,
    ): Page<SavedArticleResponse> =
        savedArticleRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { SavedArticleResponse.from(it) }

    @Transactional
    fun saveArticle(
        userId: Long,
        articleExternalId: String,
    ) {
        if (savedArticleRepository.existsByUserIdAndArticle_ExternalId(userId, articleExternalId)) {
            return // Already saved, idempotent
        }

        val article =
            articleRepository.findByExternalId(articleExternalId)
                ?: throw IllegalArgumentException("Article not found: $articleExternalId")
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val saved =
            UserSavedArticle().apply {
                this.user = user
                this.article = article
            }
        savedArticleRepository.save(saved)
    }

    @Transactional
    fun unsaveArticle(
        userId: Long,
        articleExternalId: String,
    ) {
        savedArticleRepository.deleteByUserIdAndArticle_ExternalId(userId, articleExternalId)
    }

    fun isArticleSaved(
        userId: Long,
        articleExternalId: String,
    ): Boolean = savedArticleRepository.existsByUserIdAndArticle_ExternalId(userId, articleExternalId)

    // --- Read History ---

    fun getReadHistory(
        userId: Long,
        pageable: Pageable,
    ): Page<ReadHistoryResponse> =
        readHistoryRepository
            .findByUserIdOrderByReadAtDesc(userId, pageable)
            .map { ReadHistoryResponse.from(it) }

    @Transactional
    fun trackArticleView(
        userId: Long,
        articleExternalId: String,
    ) {
        val article =
            articleRepository.findByExternalId(articleExternalId)
                ?: throw IllegalArgumentException("Article not found: $articleExternalId")

        val existing = readHistoryRepository.findByUserIdAndArticle_Id(userId, article.id!!)
        if (existing != null) {
            existing.updateReadAt()
        } else {
            val user =
                userRepository
                    .findById(userId)
                    .orElseThrow { IllegalArgumentException("User not found: $userId") }
            val history =
                UserReadHistory().apply {
                    this.user = user
                    this.article = article
                }
            readHistoryRepository.save(history)
        }
    }
}
