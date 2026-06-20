package kr.devport.api.domain.wiki.service

import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository
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
    private val openAIClient: OpenAIClient,
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

            val messages =
                listOf<ChatCompletionMessageParam>(
                    ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(prompt).build(),
                    ),
                )

            val completion =
                openAIClient.chat().completions().create(
                    ChatCompletionCreateParams
                        .builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .messages(messages)
                        .maxCompletionTokens(30L)
                        .build(),
                )

            val title = completion.choices().first().message().content().orElse("").trim()
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
}
