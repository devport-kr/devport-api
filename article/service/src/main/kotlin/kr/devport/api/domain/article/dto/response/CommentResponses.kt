package kr.devport.api.domain.article.dto.response

import kr.devport.api.domain.article.entity.ArticleComment
import kr.devport.api.domain.auth.entity.User
import java.time.LocalDateTime

data class CommentAuthorResponse(
    val id: Long? = null,
    val name: String? = null,
    val profileImageUrl: String? = null,
    val flair: String? = null,
    val flairColor: String? = null,
) {
    companion object {
        @JvmStatic
        fun from(user: User): CommentAuthorResponse =
            CommentAuthorResponse(
                id = user.id,
                name = user.name,
                profileImageUrl = user.profileImageUrl,
                flair = user.flair,
                flairColor = user.flairColor,
            )
    }
}

data class CommentResponse(
    val id: String? = null,
    val content: String? = null,
    val deleted: Boolean? = null,
    val parentId: String? = null,
    val author: CommentAuthorResponse? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val isOwner: Boolean? = null,
) {
    companion object {
        @JvmStatic
        fun from(
            comment: ArticleComment,
            currentUserId: Long?,
        ): CommentResponse {
            val displayContent = if (comment.deleted) "[삭제된 댓글입니다]" else comment.content
            return CommentResponse(
                id = comment.externalId,
                content = displayContent,
                deleted = comment.deleted,
                parentId = comment.parentComment?.externalId,
                author = CommentAuthorResponse.from(comment.user!!),
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                isOwner = currentUserId != null && currentUserId == comment.user?.id,
            )
        }
    }
}
