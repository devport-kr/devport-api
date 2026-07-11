package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.infrastructure.ChatMessage
import kr.devport.api.domain.wiki.infrastructure.ChatPort
import kr.devport.api.domain.wiki.infrastructure.ChatRole
import kr.devport.api.domain.wiki.infrastructure.WikiChatSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Async LLM title generation for wiki chat sessions: a concise 5-7 word title from the first
 * question. Fails silently — never breaks the user's chat.
 */
@Service
class WikiChatTitleService(
    private val sessionRepository: WikiChatSessionRepository,
    private val chatPort: ChatPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional
    fun generateAndSave(
        sessionExternalId: String,
        firstQuestion: String,
    ) {
        try {
            val prompt =
                "다음 첫 질문으로 시작하는 대화의 제목을 5~7단어로 간결하게 한국어로 작성하세요: '" +
                    firstQuestion + "'. 제목만 출력하고 따옴표나 마침표는 붙이지 마세요."

            val title =
                chatPort
                    .complete(
                        model = CHAT_MODEL,
                        messages = listOf(ChatMessage(ChatRole.USER, prompt)),
                        maxCompletionTokens = 30L,
                    ).trim()

            if (title.isNotBlank()) {
                sessionRepository.findByExternalId(sessionExternalId).ifPresent { session ->
                    session.title = title
                    sessionRepository.save(session)
                }
            }
        } catch (e: Exception) {
            log.warn("wiki-title: Failed to generate title for session={}", sessionExternalId, e)
        }
    }

    companion object {
        private const val CHAT_MODEL = "gpt-4o-mini"
    }
}
