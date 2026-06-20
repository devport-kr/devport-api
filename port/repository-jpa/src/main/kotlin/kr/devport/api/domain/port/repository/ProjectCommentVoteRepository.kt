@file:Suppress("ktlint:standard:function-naming")

package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.ProjectCommentVote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ProjectCommentVoteRepository : JpaRepository<ProjectCommentVote, Long> {
    fun findByComment_ExternalIdAndUser_Id(
        commentExternalId: String,
        userId: Long,
    ): Optional<ProjectCommentVote>

    @Query("SELECT SUM(v.vote) FROM ProjectCommentVote v WHERE v.comment.externalId = :commentExternalId")
    fun calculateVoteScore(
        @Param("commentExternalId") commentExternalId: String,
    ): Int?

    fun deleteByComment_ExternalIdAndUser_Id(
        commentExternalId: String,
        userId: Long,
    )
}
