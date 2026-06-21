package kr.devport.api.domain.mypage.repository

import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import kr.devport.api.domain.mypage.infrastructure.UserReadHistoryRepository
import kr.devport.api.domain.mypage.infrastructure.UserSavedArticleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserSavedArticleRepositoryAdapter(
    private val jpa: UserSavedArticleJpaRepository,
) : UserSavedArticleRepository {
    override fun findByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserSavedArticle> = jpa.findByUserIdOrderByCreatedAtDesc(userId, pageable)

    override fun existsByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): Boolean = jpa.existsByUserIdAndArticleId(userId, articleId)

    override fun deleteByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ) = jpa.deleteByUserIdAndArticleId(userId, articleId)

    override fun save(saved: UserSavedArticle): UserSavedArticle = jpa.save(saved)
}

@Repository
class UserReadHistoryRepositoryAdapter(
    private val jpa: UserReadHistoryJpaRepository,
) : UserReadHistoryRepository {
    override fun findByUserIdOrderByReadAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserReadHistory> = jpa.findByUserIdOrderByReadAtDesc(userId, pageable)

    override fun findByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): UserReadHistory? = jpa.findByUserIdAndArticleId(userId, articleId)

    override fun save(history: UserReadHistory): UserReadHistory = jpa.save(history)
}
