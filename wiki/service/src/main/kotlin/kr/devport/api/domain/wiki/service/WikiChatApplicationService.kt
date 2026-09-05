package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest
import kr.devport.api.domain.wiki.dto.request.WikiGlobalChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.function.Consumer

/**
 * Orchestrates wiki chat: applies anon/authenticated rate limits then delegates to the project or
 * global chat use-cases. Callers are identified by [userId] (null = anonymous).
 */
@Service
class WikiChatApplicationService(
    private val wikiChatService: WikiChatService,
    private val wikiGlobalChatService: WikiGlobalChatService,
    private val rateLimiter: WikiChatRateLimiter,
    private val anonRateLimiter: WikiAnonRateLimiter,
    @param:Value("\${wiki.chat.rate-limit.enabled:true}") private val rateLimitEnabled: Boolean,
) {
    fun chatProject(
        projectExternalId: String,
        request: WikiChatRequest,
        userId: Long?,
        clientIp: String?,
    ): WikiChatResponse {
        applyRateLimit(userId, clientIp)
        val result = wikiChatService.chatResult(request.sessionId, projectExternalId, request.question, userId)
        return WikiChatResponse.from(result, request.sessionId)
    }

    fun streamProject(
        projectExternalId: String,
        request: WikiChatRequest,
        userId: Long?,
        clientIp: String?,
        tokenConsumer: Consumer<String>,
    ): WikiChatResult {
        applyRateLimit(userId, clientIp)
        return wikiChatService.streamChatResult(
            request.sessionId,
            projectExternalId,
            request.question,
            tokenConsumer,
            userId,
        )
    }

    fun chatGlobal(
        request: WikiGlobalChatRequest,
        userId: Long?,
        clientIp: String?,
    ): WikiGlobalChatResponse {
        applyRateLimit(userId, clientIp)
        return wikiGlobalChatService.chatResult(request.sessionId, request.question, userId)
    }

    fun streamGlobal(
        request: WikiGlobalChatRequest,
        userId: Long?,
        clientIp: String?,
        tokenConsumer: Consumer<String>,
    ): WikiGlobalChatResponse {
        applyRateLimit(userId, clientIp)
        return wikiGlobalChatService.streamChatResult(request.sessionId, request.question, tokenConsumer, userId)
    }

    fun clearProjectSession(sessionId: String) {
        wikiChatService.clearSession(sessionId)
    }

    private fun applyRateLimit(
        userId: Long?,
        clientIp: String?,
    ) {
        if (!rateLimitEnabled) {
            return
        }
        if (userId == null) {
            anonRateLimiter.checkAndIncrement(clientIp ?: "")
        } else {
            rateLimiter.check(userId.toString())
        }
    }
}
