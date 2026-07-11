package kr.devport.api.domain.wiki.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kr.devport.api.domain.common.logging.LoggingContext
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.wiki.dto.request.WikiGlobalChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse
import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException
import kr.devport.api.domain.wiki.service.WikiChatApplicationService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Global wiki chat controller — discovers relevant projects across all wikis. Supports authenticated
 * and anonymous users (anon: 1 req/day via IP limit).
 */
@RestController
@RequestMapping("/api/wiki/chat")
class WikiGlobalChatController(
    private val chatApplicationService: WikiChatApplicationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val streamExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    @PostMapping
    fun chat(
        @Valid @RequestBody request: WikiGlobalChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<WikiGlobalChatResponse> =
        ResponseEntity.ok(
            chatApplicationService.chatGlobal(
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest),
            ),
        )

    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @Valid @RequestBody request: WikiGlobalChatRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        httpRequest: HttpServletRequest,
    ): SseEmitter {
        val userId = extractUserId(userDetails)
        val clientIp = extractClientIp(userDetails, httpRequest)

        val emitter = SseEmitter(STREAM_TIMEOUT_MILLIS)
        emitter.onTimeout { emitter.complete() }

        streamExecutor.execute(
            LoggingContext.wrap {
                try {
                    val response =
                        chatApplicationService.streamGlobal(
                            request,
                            userId,
                            clientIp,
                            { token -> sendToken(emitter, token) },
                        )
                    emitter.send(SseEmitter.event().name("done").data(response, MediaType.APPLICATION_JSON))
                    emitter.complete()
                } catch (ex: Exception) {
                    handleStreamFailure(emitter, ex)
                }
            },
        )

        return emitter
    }

    @PreDestroy
    fun shutdownStreamExecutor() {
        streamExecutor.close()
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
        log.error("Wiki global chat stream failed", ex)
        try {
            val message = if (ex is WikiChatRateLimitExceededException) ex.message else STREAM_ERROR_MESSAGE
            emitter.send(SseEmitter.event().name("error").data(mapOf("message" to message), MediaType.APPLICATION_JSON))
        } catch (ignored: IOException) {
        }
        emitter.complete()
    }

    companion object {
        private const val STREAM_TIMEOUT_MILLIS = 120_000L
        private const val STREAM_ERROR_MESSAGE = "처리 중 오류가 발생했습니다."
        private val OBJECT_MAPPER = ObjectMapper()
    }
}
