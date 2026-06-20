package kr.devport.api.domain.port.service

import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.port.dto.request.CommentCreateRequest
import kr.devport.api.domain.port.dto.request.CommentUpdateRequest
import kr.devport.api.domain.port.dto.request.VoteRequest
import kr.devport.api.domain.port.dto.response.ProjectCommentResponse
import kr.devport.api.domain.port.dto.response.VoteResponse
import kr.devport.api.domain.port.entity.ProjectComment
import kr.devport.api.domain.port.entity.ProjectCommentVote
import kr.devport.api.domain.port.repository.ProjectCommentRepository
import kr.devport.api.domain.port.repository.ProjectCommentVoteRepository
import kr.devport.api.domain.port.repository.ProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProjectCommentService(
    private val commentRepository: ProjectCommentRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val voteRepository: ProjectCommentVoteRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getCommentsByProject(
        projectExternalId: String,
        currentUserId: Long?,
    ): List<ProjectCommentResponse> =
        commentRepository
            .findAllByProjectExternalId(projectExternalId)
            .map { it.toCommentResponse(currentUserId, findUserVote(it.externalId, currentUserId)) }

    @Transactional
    fun createComment(
        projectExternalId: String,
        request: CommentCreateRequest,
        userId: Long,
    ): ProjectCommentResponse {
        val project =
            projectRepository
                .findByExternalId(projectExternalId)
                .orElseThrow { IllegalArgumentException("Project not found: $projectExternalId") }
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val comment =
            ProjectComment().apply {
                this.project = project
                this.user = user
                content = request.content
            }

        request.parentCommentId?.let { parentCommentId ->
            val parent =
                commentRepository
                    .findByExternalId(parentCommentId)
                    .orElseThrow { IllegalArgumentException("Parent comment not found: $parentCommentId") }

            if (parent.project?.id != project.id) {
                throw IllegalArgumentException("Parent comment does not belong to this project")
            }
            comment.parentComment = parent
        }

        val saved = commentRepository.save(comment)
        log.info("Created comment {} for project {}", saved.externalId, projectExternalId)
        return saved.toCommentResponse(userId, userVote = 0)
    }

    @Transactional
    fun updateComment(
        commentExternalId: String,
        request: CommentUpdateRequest,
        userId: Long,
    ): ProjectCommentResponse {
        val comment =
            commentRepository
                .findByExternalId(commentExternalId)
                .orElseThrow { IllegalArgumentException("Comment not found: $commentExternalId") }

        if (comment.user?.id != userId) {
            throw IllegalArgumentException("You can only edit your own comments")
        }
        if (comment.deleted) {
            throw IllegalArgumentException("Cannot edit a deleted comment")
        }

        comment.content = request.content
        comment.updatedAt = LocalDateTime.now()
        val updated = commentRepository.save(comment)
        log.info("Updated comment {}", commentExternalId)
        return updated.toCommentResponse(userId, findUserVote(commentExternalId, userId))
    }

    @Transactional
    fun deleteComment(
        commentExternalId: String,
        userId: Long,
    ) {
        val comment =
            commentRepository
                .findByExternalId(commentExternalId)
                .orElseThrow { IllegalArgumentException("Comment not found: $commentExternalId") }

        if (comment.user?.id != userId) {
            throw IllegalArgumentException("You can only delete your own comments")
        }

        comment.deleted = true
        comment.updatedAt = LocalDateTime.now()
        commentRepository.save(comment)
        log.info("Deleted comment {}", commentExternalId)
    }

    @Transactional
    fun voteOnComment(
        commentExternalId: String,
        request: VoteRequest,
        userId: Long,
    ): VoteResponse {
        val comment =
            commentRepository
                .findByExternalId(commentExternalId)
                .orElseThrow { IllegalArgumentException("Comment not found") }
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }
        val existingVote = voteRepository.findByComment_ExternalIdAndUser_Id(commentExternalId, userId)

        if (request.vote == 0) {
            existingVote.ifPresent { vote ->
                voteRepository.delete(vote)
                updateCommentVoteScore(comment)
                log.info("Removed vote on comment {}", commentExternalId)
            }
        } else if (existingVote.isPresent) {
            val vote = existingVote.get()
            if (vote.vote == request.vote.toShort()) {
                voteRepository.delete(vote)
                log.info("Toggled off vote on comment {}", commentExternalId)
            } else {
                vote.vote = request.vote.toShort()
                voteRepository.save(vote)
                log.info("Changed vote on comment {}", commentExternalId)
            }
            updateCommentVoteScore(comment)
        } else {
            val vote =
                ProjectCommentVote().apply {
                    this.comment = comment
                    this.user = user
                    vote = request.vote.toShort()
                }
            voteRepository.save(vote)
            updateCommentVoteScore(comment)
            log.info("Added new vote on comment {}", commentExternalId)
        }

        val score = voteRepository.calculateVoteScore(commentExternalId) ?: 0
        return VoteResponse(votes = score, userVote = request.vote)
    }

    private fun updateCommentVoteScore(comment: ProjectComment) {
        val externalId = requireNotNull(comment.externalId)
        comment.voteScore = voteRepository.calculateVoteScore(externalId) ?: 0
        commentRepository.save(comment)
    }

    private fun findUserVote(
        commentExternalId: String?,
        currentUserId: Long?,
    ): Int {
        if (commentExternalId == null || currentUserId == null) {
            return 0
        }

        return voteRepository
            .findByComment_ExternalIdAndUser_Id(commentExternalId, currentUserId)
            .map { it.vote.toInt() }
            .orElse(0)
    }
}
