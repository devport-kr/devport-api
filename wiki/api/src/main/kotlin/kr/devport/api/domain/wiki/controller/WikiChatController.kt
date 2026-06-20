package kr.devport.api.domain.wiki.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kr.devport.api.domain.common.logging.LoggingContext
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse
import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.service.WikiChatApplicationService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Wiki chat controller. Supports authenticated and anonymous users (anon: 1 req/day via IP limit).
 */
@RestController
@RequestMapping("/api/wiki/projects")
class WikiChatController(
    private val chatApplicationService: WikiChatApplicationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val streamExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    @PostMapping("/{projectExternalId}/chat")
    fun chat(
        @PathVariable projectExternalId: String,
        @Valid @RequestBody request: WikiChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<WikiChatResponse> =
        ResponseEntity.ok(
            chatApplicationService.chatProject(
                projectExternalId,
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest),
            ),
        )

    @PostMapping("/chat")
    fun chatByQueryId(
        @RequestParam id: String,
        @Valid @RequestBody request: WikiChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<WikiChatResponse> =
        ResponseEntity.ok(
            chatApplicationService.chatProject(
                id,
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest),
            ),
        )

    @PostMapping("/{projectExternalId}/chat/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @PathVariable projectExternalId: String,
        @Valid @RequestBody request: WikiChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): SseEmitter = streamChatInternal(projectExternalId, request, userDetails, httpRequest)

    @PostMapping("/chat/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChatByQueryId(
        @RequestParam id: String,
        @Valid @RequestBody request: WikiChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): SseEmitter = streamChatInternal(id, request, userDetails, httpRequest)

    @DeleteMapping("/{projectExternalId}/chat/sessions/{sessionId}")
    fun clearSession(
        @PathVariable projectExternalId: String,
        @PathVariable sessionId: String,
    ): ResponseEntity<Void> {
        chatApplicationService.clearProjectSession(sessionId)
        return ResponseEntity.noContent().build()
    }

    @PreDestroy
    fun shutdownStreamExecutor() {
        streamExecutor.close()
    }

    private fun streamChatInternal(
        projectExternalId: String,
        request: WikiChatRequest,
        userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): SseEmitter {
        // HttpServletRequest is not safe to use inside async threads — extract synchronously.
        val userId = extractUserId(userDetails)
        val clientIp = extractClientIp(userDetails, httpRequest)

        val emitter = SseEmitter(STREAM_TIMEOUT_MILLIS)
        emitter.onTimeout { emitter.complete() }

        streamExecutor.execute(
            LoggingContext.wrap {
                try {
                    val result =
                        chatApplicationService.streamProject(
                            projectExternalId,
                            request,
                            userId,
                            clientIp,
                            { token -> sendToken(emitter, token) },
                        )
                    emitter.send(
                        SseEmitter
                            .event()
                            .name("done")
                            .data(
                                StreamDonePayload(
                                    request.sessionId,
                                    result.isClarification,
                                    result.clarificationOptions,
                                    result.suggestedNextQuestions,
                                    result.sessionReset,
                                ),
                                MediaType.APPLICATION_JSON,
                            ),
                    )
                    emitter.complete()
                } catch (ex: Exception) {
                    handleStreamFailure(emitter, ex)
                }
            },
        )

        return emitter
    }

    private fun extractUserId(userDetails: CustomUserDetails?): Long? = userDetails?.id

    private fun extractClientIp(
        userDetails: CustomUserDetails?,
        request: HttpServletRequest,
    ): String? = if (userDetails == null) extractIp(request) else null

    private fun extractIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",")[0].trim()
        }
        return request.remoteAddr
    }

    private fun sendToken(
        emitter: SseEmitter,
        token: String?,
    ) {
        try {
            if (token != null) {
                val jsonToken = OBJECT_MAPPER.writeValueAsString(token)
                emitter.send(SseEmitter.event().name("token").data(jsonToken, MediaType.APPLICATION_JSON))
            }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to send streaming token", e)
        }
    }

    private fun handleStreamFailure(
        emitter: SseEmitter,
        ex: Exception,
    ) {
        log.error("Wiki chat stream failed", ex)
        try {
            val message =
                if (ex is WikiChatRateLimitExceededException) {
                    ex.message
                } else {
                    "서비스 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                }
            emitter.send(SseEmitter.event().name("error").data(mapOf("message" to message), MediaType.APPLICATION_JSON))
        } catch (ignored: IOException) {
        }
        emitter.complete()
    }

    data class StreamDonePayload(
        val sessionId: String?,
        @get:JsonProperty("isClarification")
        val isClarification: Boolean,
        val clarificationOptions: List<String>?,
        val suggestedNextQuestions: List<String>?,
        val sessionReset: Boolean,
    )

    companion object {
        private const val STREAM_TIMEOUT_MILLIS = 120_000L
        private val OBJECT_MAPPER = ObjectMapper()
    }
}
