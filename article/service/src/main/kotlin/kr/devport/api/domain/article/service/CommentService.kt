package kr.devport.api.domain.article.service

import kr.devport.api.domain.article.dto.request.CommentCreateRequest
import kr.devport.api.domain.article.dto.request.CommentUpdateRequest
import kr.devport.api.domain.article.dto.response.CommentResponse
import kr.devport.api.domain.article.entity.ArticleComment
import kr.devport.api.domain.article.repository.ArticleCommentRepository
import kr.devport.api.domain.article.repository.ArticleRepository
import kr.devport.api.domain.auth.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: ArticleCommentRepository,
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository,
) {
    fun getCommentsByArticle(
        articleExternalId: String,
        currentUserId: Long?,
    ): List<CommentResponse> =
        commentRepository
            .findAllByArticleExternalId(articleExternalId)
            .map { CommentResponse.from(it, currentUserId) }

    @Transactional
    fun createComment(
        articleExternalId: String,
        request: CommentCreateRequest,
        userId: Long,
    ): CommentResponse {
        val article =
            articleRepository.findByExternalId(articleExternalId)
                ?: throw IllegalArgumentException("Article not found: $articleExternalId")
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val comment =
            ArticleComment().apply {
                this.article = article
                this.user = user
                this.content = request.content
            }

        request.parentCommentId?.let { parentId ->
            val parent =
                commentRepository.findByExternalId(parentId)
                    ?: throw IllegalArgumentException("Parent comment not found: $parentId")
            if (parent.article?.id != article.id) {
                throw IllegalArgumentException("Parent comment does not belong to this article")
            }
            comment.parentComment = parent
        }

        val saved = commentRepository.save(comment)
        return CommentResponse.from(saved, userId)
    }

    @Transactional
    fun updateComment(
        commentExternalId: String,
        request: CommentUpdateRequest,
        userId: Long,
    ): CommentResponse {
        val comment =
            commentRepository.findByExternalId(commentExternalId)
                ?: throw IllegalArgumentException("Comment not found: $commentExternalId")

        if (comment.user?.id != userId) {
            throw IllegalArgumentException("You can only edit your own comments")
        }
        if (comment.deleted) {
            throw IllegalArgumentException("Cannot edit a deleted comment")
        }

        comment.content = request.content
        val updated = commentRepository.save(comment)
        return CommentResponse.from(updated, userId)
    }

    @Transactional
    fun deleteComment(
        commentExternalId: String,
        userId: Long,
    ) {
        val comment =
            commentRepository.findByExternalId(commentExternalId)
                ?: throw IllegalArgumentException("Comment not found: $commentExternalId")

        if (comment.user?.id != userId) {
            throw IllegalArgumentException("You can only delete your own comments")
        }

        comment.deleted = true
        commentRepository.save(comment)
    }
}
