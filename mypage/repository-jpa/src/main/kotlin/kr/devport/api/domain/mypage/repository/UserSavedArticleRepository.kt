@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.mypage.repository

import kr.devport.api.domain.mypage.entity.UserSavedArticle
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserSavedArticleRepository : JpaRepository<UserSavedArticle, Long> {
    @Query(
        """
        SELECT s FROM UserSavedArticle s
        JOIN FETCH s.article
        WHERE s.user.id = :userId
        ORDER BY s.createdAt DESC
        """,
    )
    fun findByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<UserSavedArticle>

    fun findByUserIdAndArticle_ExternalId(
        userId: Long,
        articleExternalId: String,
    ): UserSavedArticle?

    fun existsByUserIdAndArticle_ExternalId(
        userId: Long,
        articleExternalId: String,
    ): Boolean

    fun deleteByUserIdAndArticle_ExternalId(
        userId: Long,
        articleExternalId: String,
    )
}
