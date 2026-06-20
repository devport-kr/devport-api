package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest
import kr.devport.api.domain.wiki.dto.request.WikiGlobalChatRequest
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.function.Consumer

/**
 * Orchestrates wiki chat: resolves the caller (applying anon/authenticated rate limits) then
 * delegates to the project or global chat use-cases.
 */
@Service
class WikiChatApplicationService(
    private val wikiChatService: WikiChatService,
    private val wikiGlobalChatService: WikiGlobalChatService,
    private val rateLimiter: WikiChatRateLimiter,
    private val anonRateLimiter: WikiAnonRateLimiter,
    private val userRepository: UserRepository,
    @param:Value("\${wiki.chat.rate-limit.enabled:true}") private val rateLimitEnabled: Boolean,
) {
    fun chatProject(
        projectExternalId: String,
        request: WikiChatRequest,
        userId: Long?,
        clientIp: String?,
    ): WikiChatResponse {
        val user = resolveUser(userId, clientIp)
        val result = wikiChatService.chatResult(request.sessionId, projectExternalId, request.question, user)
        return WikiChatResponse.from(result, request.sessionId)
    }

    fun streamProject(
        projectExternalId: String,
        request: WikiChatRequest,
        userId: Long?,
        clientIp: String?,
        tokenConsumer: Consumer<String>,
    ): WikiChatResult {
        val user = resolveUser(userId, clientIp)
        return wikiChatService.streamChatResult(
            request.sessionId,
            projectExternalId,
            request.question,
            tokenConsumer,
            user,
        )
    }

    fun chatGlobal(
        request: WikiGlobalChatRequest,
        userId: Long?,
        clientIp: String?,
    ): WikiGlobalChatResponse {
        val user = resolveUser(userId, clientIp)
        return wikiGlobalChatService.chatResult(request.sessionId, request.question, user)
    }

    fun streamGlobal(
        request: WikiGlobalChatRequest,
        userId: Long?,
        clientIp: String?,
        tokenConsumer: Consumer<String>,
    ): WikiGlobalChatResponse {
        val user = resolveUser(userId, clientIp)
        return wikiGlobalChatService.streamChatResult(request.sessionId, request.question, tokenConsumer, user)
    }

    fun clearProjectSession(sessionId: String) {
        wikiChatService.clearSession(sessionId)
    }

    private fun resolveUser(
        userId: Long?,
        clientIp: String?,
    ): User? {
        if (userId == null) {
            if (rateLimitEnabled) {
                anonRateLimiter.checkAndIncrement(clientIp ?: "")
            }
            return null
        }
        if (rateLimitEnabled) {
            rateLimiter.check(userId.toString())
        }
        return userRepository.findById(userId).orElse(null)
    }
}
