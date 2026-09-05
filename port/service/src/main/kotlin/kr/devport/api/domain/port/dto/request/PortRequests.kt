package kr.devport.api.domain.port.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentCreateRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000, message = "Content must be 5000 characters or less")
    val content: String = "",
    val parentCommentId: String? = null,
)

data class CommentUpdateRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000, message = "Content must be 5000 characters or less")
    val content: String = "",
)

data class VoteRequest(
    @field:Min(value = -1, message = "Vote must be -1, 0, or 1")
    @field:Max(value = 1, message = "Vote must be -1, 0, or 1")
    val vote: Int = 0,
)
