package kr.devport.api.domain.mypage.repository

import kr.devport.api.domain.mypage.entity.UserReadHistory
import kr.devport.api.domain.mypage.entity.UserSavedArticle
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

// Spring Data interfaces — internal to this adapter module. Articles are referenced by id;
// the core depends on the ports in :mypage:infrastructure via the adapters below.

interface UserSavedArticleJpaRepository : JpaRepository<UserSavedArticle, Long> {
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
}

interface UserReadHistoryJpaRepository : JpaRepository<UserReadHistory, Long> {
    fun findByUserIdOrderByReadAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<UserReadHistory>

    fun findByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): UserReadHistory?
}
