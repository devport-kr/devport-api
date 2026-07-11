package kr.devport.api.domain.mypage.infrastructure

import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** Out-ports: mypage persistence contracts. Articles/users are referenced by id (cross-domain). */
interface UserSavedArticleRepository {
    fun findByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserSavedArticle>

    fun existsByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): Boolean

    fun deleteByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    )

    fun save(saved: UserSavedArticle): UserSavedArticle
}

interface UserReadHistoryRepository {
    fun findByUserIdOrderByReadAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserReadHistory>

    fun findByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): UserReadHistory?

    fun save(history: UserReadHistory): UserReadHistory
}
