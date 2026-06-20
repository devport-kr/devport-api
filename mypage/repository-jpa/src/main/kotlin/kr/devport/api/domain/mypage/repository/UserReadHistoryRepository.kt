@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.mypage.repository

import kr.devport.api.domain.mypage.entity.UserReadHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserReadHistoryRepository : JpaRepository<UserReadHistory, Long> {
    @Query(
        """
        SELECT h FROM UserReadHistory h
        JOIN FETCH h.article
        WHERE h.user.id = :userId
        ORDER BY h.readAt DESC
        """,
    )
    fun findByUserIdOrderByReadAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<UserReadHistory>

    fun findByUserIdAndArticle_Id(
        userId: Long,
        articleId: Long,
    ): UserReadHistory?
}
