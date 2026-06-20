@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.article.repository

import kr.devport.api.domain.article.entity.ArticleComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ArticleCommentRepository : JpaRepository<ArticleComment, Long> {
    fun findByExternalId(externalId: String): ArticleComment?

    @Query(
        """
        SELECT c FROM ArticleComment c
        JOIN FETCH c.user
        WHERE c.article.externalId = :articleExternalId
        ORDER BY c.createdAt ASC
        """,
    )
    fun findAllByArticleExternalId(
        @Param("articleExternalId") articleExternalId: String,
    ): List<ArticleComment>

    @Query("SELECT COUNT(c) FROM ArticleComment c WHERE c.article.externalId = :articleExternalId AND c.deleted = false")
    fun countByArticleExternalId(
        @Param("articleExternalId") articleExternalId: String,
    ): Long

    fun existsByParentComment_Id(parentId: Long): Boolean
}
