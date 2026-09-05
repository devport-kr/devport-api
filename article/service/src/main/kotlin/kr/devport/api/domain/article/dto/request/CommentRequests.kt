package kr.devport.api.domain.article.dto.request

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
