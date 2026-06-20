package kr.devport.api.domain.wiki.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class WikiChatRequest(
    @field:NotBlank(message = "Question is required")
    @field:Size(max = 1000, message = "Question must not exceed 1000 characters")
    val question: String = "",
    @field:NotBlank(message = "Session ID is required")
    val sessionId: String = "",
)

data class WikiGlobalChatRequest(
    @field:NotBlank(message = "Question is required")
    @field:Size(max = 1000, message = "Question must not exceed 1000 characters")
    val question: String = "",
    @field:NotBlank(message = "Session ID is required")
    val sessionId: String = "",
)
