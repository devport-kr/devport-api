package kr.devport.api.domain.wiki.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.client.OpenAIClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionChunk
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import kr.devport.api.domain.wiki.store.WikiChatSessionStore
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * Wiki chat with uncertainty handling and session-scoped memory. Returns clarifying questions when
 * confidence is low instead of fabricated answers. Session memory is short-lived.
 */
@Service
class WikiChatService(
    private val retrievalService: WikiRetrievalService,
    private val sessionStore: WikiChatSessionStore,
    private val persistenceService: WikiChatSessionPersistenceService,
    private val titleService: WikiChatTitleService,
    private val openAIClient: OpenAIClient,
) {
    private val objectMapper = ObjectMapper()

    fun chat(
        sessionId: String,
        projectExternalId: String,
        userQuestion: String,
    ): String = chatResult(sessionId, projectExternalId, userQuestion, null).answer ?: ""

    @JvmOverloads
    fun chatResult(
        sessionId: String,
        projectExternalId: String,
        userQuestion: String,
        user: User? = null,
    ): WikiChatResult {
        val chatRequest = prepareChatRequest(sessionId, projectExternalId, userQuestion, user)
        val messages = buildMessages(chatRequest.context, chatRequest.promptTurns, userQuestion, chatRequest.clarificationTurns, false)
        val completion =
            openAIClient.chat().completions().create(
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.GPT_5_MINI)
                    .messages(messages)
                    .responseFormat(buildResponseFormat())
                    .build(),
            )

        val payload = completion.choices().first().message().content().orElse("")
        val result =
            normalizeResult(
                parseResult(payload, chatRequest.context),
                chatRequest.context,
                chatRequest.topicShift,
                chatRequest.sessionReset,
                chatRequest.clarificationTurns,
            )

        persist(sessionId, projectExternalId, userQuestion, result, user)
        return result
    }

    @JvmOverloads
    fun streamChatResult(
        sessionId: String,
        projectExternalId: String,
        userQuestion: String,
        tokenConsumer: Consumer<String>,
        user: User? = null,
    ): WikiChatResult {
        val chatRequest = prepareChatRequest(sessionId, projectExternalId, userQuestion, user)
        val messages = buildMessages(chatRequest.context, chatRequest.promptTurns, userQuestion, chatRequest.clarificationTurns, true)
        val accumulated = StringBuilder()

        openAIClient
            .chat()
            .completions()
            .createStreaming(
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.GPT_5_MINI)
                    .messages(messages)
                    .build(),
            ).use { completionStream ->
                completionStream.stream().use { chunks ->
                    chunks.forEach { chunk -> appendStreamChunk(chunk, tokenConsumer, accumulated) }
                }
            }

        val result =
            normalizeStreamedResult(
                accumulated.toString(),
                chatRequest,
                !chatRequest.topicShift && chatRequest.promptTurns.isNotEmpty(),
            )

        persist(sessionId, projectExternalId, userQuestion, result, user)
        return result
    }

    private fun persist(
        sessionId: String,
        projectExternalId: String,
        userQuestion: String,
        result: WikiChatResult,
        user: User?,
    ) {
        if (user != null) {
            val isFirst = persistenceService.isFirstMessage(sessionId)
            val session = persistenceService.findOrCreateSession(sessionId, user, projectExternalId, WikiChatSessionType.PROJECT)
            persistenceService.saveUserMessage(session, userQuestion)
            persistenceService.saveAssistantMessage(session, result.answer ?: "", result.isClarification)
            if (isFirst) {
                titleService.generateAndSave(sessionId, userQuestion)
            }
        }
        sessionStore.saveTurn(sessionId, projectExternalId, userQuestion, result.answer ?: "", result.isClarification)
    }

    private fun buildMessages(
        context: WikiRetrievalContext,
        previousTurns: List<ChatTurn>,
        userQuestion: String,
        clarificationTurns: Int,
        streaming: Boolean,
    ): List<ChatCompletionMessageParam> {
        val messages = mutableListOf<ChatCompletionMessageParam>()

        val systemPrompt = buildSystemPrompt(context, clarificationTurns > 0, streaming)
        messages.add(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(systemPrompt).build(),
            ),
        )

        for (turn in previousTurns) {
            messages.add(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.question ?: "").build(),
                ),
            )
            messages.add(
                ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder().content(turn.answer ?: "").build(),
                ),
            )
        }

        messages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam
                    .builder()
                    .content(buildUserPrompt(context, userQuestion, clarificationTurns, streaming))
                    .build(),
            ),
        )

        return messages
    }

    private fun buildSystemPrompt(
        context: WikiRetrievalContext,
        hasPreviousContext: Boolean,
        streaming: Boolean,
    ): String {
        if (streaming) {
            return """
                You are a repository-grounded technical teammate.
                Output in rich, readable Markdown format.
                Do not output JSON.
                Always write the answer in Korean only.

                Markdown Formatting Policy:
                - Use fenced code blocks with language identifiers (e.g., ```java, ```bash) for multi-line code snippets or command outputs.
                - Use single backticks (`) for inline code, variables, or file paths.
                - Use bolding (**text**) for emphasis on key technical terms.
                - Use bullet points (-) or numbered lists to break down complex explanations or steps.
                - You MUST insert an empty line between every bullet point, numbered list item, and paragraph to ensure extreme readability.

                Answer policy:
                - Stay concise, direct, and repository-specific.
                - Mention file paths, classes, or methods only when grounded context supports them.
                - If the question is broad but partly answerable, answer the safest slice first and then add one narrow clarification.
                - If the question is ambiguous across repo areas, write %s on its own line and follow it with 2-3 short - bullet options without question marks.
                - If grounding is weak, keep the answer short and action-oriented, then write %s on its own line and follow it with 2-3 short - bullet suggestions.
                - Do not add confidence labels, citations, JSON, or generic chat filler.

                Conversation policy:
                - Recent turns are already filtered to the most relevant context.
                - Do not repeat stale earlier topics unless they are clearly needed.
                - When previous clarification exists, continue naturally without looping.

                Grounding strength: %s
                Previous context included: %s

                Repository Context:
                %s
                """.trimIndent().format(
                CLARIFICATION_HEADING,
                SUGGESTED_QUESTION_HEADING,
                if (context.weakGrounding) "weak" else "strong",
                hasPreviousContext,
                context.groundedContext,
            )
        }

        return """
            You are a repository-grounded technical teammate.
            Output must be valid JSON matching the schema.
            Format the answer field in rich, readable Markdown.
            Always write the answer in Korean only.

            Markdown Formatting Policy:
            - Use fenced code blocks with language identifiers (e.g., ```java, ```bash) for multi-line code snippets or command outputs.
            - Use single backticks (`) for inline code, variables, or file paths.
            - Use bolding (**text**) for emphasis on key technical terms.
            - Use bullet points (-) or numbered lists to break down complex explanations or steps.
            - You MUST insert an empty line between every bullet point, numbered list item, and paragraph to ensure extreme readability.

            Answer policy:
            - Stay concise, direct, and repository-specific.
            - Mention file paths, classes, or methods only when grounded context supports them.
            - If the question is broad but partly answerable, answer the safest slice first and then add one narrow clarification.
            - If the question is ambiguous across repo areas, set isClarification=true and return 2-3 short clarificationOptions without question marks.
            - If grounding is weak, keep the answer short and action-oriented and include 2-3 suggestedNextQuestions.
            - Do not add confidence labels, citations, or generic chat filler.
            - usedPreviousContext should be true only when recent turns materially help this answer.

            Conversation policy:
            - Recent turns are already filtered to the most relevant context.
            - Do not repeat stale earlier topics unless they are clearly needed.
            - When previous clarification exists, continue naturally without looping.

            Grounding strength: %s
            Previous context included: %s

            Repository Context:
            %s
            """.trimIndent().format(if (context.weakGrounding) "weak" else "strong", hasPreviousContext, context.groundedContext)
    }

    private fun buildUserPrompt(
        context: WikiRetrievalContext,
        userQuestion: String,
        clarificationTurns: Int,
        streaming: Boolean,
    ): String {
        val responseInstruction =
            if (streaming) {
                "응답은 마크다운(Markdown)이 적용된 한국어 텍스트로 반환하세요."
            } else {
                "응답 JSON만 반환하세요."
            }
        val faqInstruction = buildFaqInstruction(userQuestion)
        return """
            질문: %s
            약한 근거 여부: %s
            기존 추천 질문: %s
            누적 clarification 턴: %d
            %s
            %s
            """.trimIndent().format(
            userQuestion,
            context.weakGrounding,
            context.suggestedNextQuestions,
            clarificationTurns,
            faqInstruction,
            responseInstruction,
        )
    }

    private fun buildFaqInstruction(question: String): String =
        when {
            FAQ_PROBLEM.matcher(question).find() ->
                "FAQ 유형: 프로젝트 목적/문제 해결. 명확화 질문 없이 이 프로젝트가 해결하는 핵심 문제와 존재 이유를 포괄적으로 답하세요."
            FAQ_ARCH.matcher(question).find() ->
                "FAQ 유형: 핵심 아키텍처. 명확화 질문 없이 전체 시스템 구조, 주요 컴포넌트, 데이터 흐름을 구조화하여 설명하세요."
            FAQ_START.matcher(question).find() ->
                "FAQ 유형: 시작 방법. 명확화 질문 없이 설치/설정/첫 실행까지의 단계를 순서대로 안내하세요."
            FAQ_CHANGES.matcher(question).find() ->
                "FAQ 유형: 최근 변경 사항. 명확화 질문 없이 최근 업데이트, 릴리즈, 주요 변경 내용을 정리해 답하세요."
            FAQ_FEATURES.matcher(question).find() ->
                "FAQ 유형: 주요 기능. 명확화 질문 없이 핵심 기능 목록을 간결하게 나열하여 설명하세요."
            else -> ""
        }

    private fun parseResult(
        payload: String,
        context: WikiRetrievalContext,
    ): WikiChatResult =
        try {
            val root = objectMapper.readTree(payload)
            WikiChatResult(
                root.path("answer").asText("").trim(),
                root.path("isClarification").asBoolean(false),
                readStringList(root.path("clarificationOptions")),
                readStringList(root.path("suggestedNextQuestions")),
                root.path("usedPreviousContext").asBoolean(false),
                false,
            )
        } catch (ignored: Exception) {
            fallbackResult(context)
        }

    private fun normalizeResult(
        raw: WikiChatResult,
        context: WikiRetrievalContext,
        topicShift: Boolean,
        sessionReset: Boolean,
        clarificationTurns: Int,
    ): WikiChatResult {
        var answer = if (raw.answer.isNullOrBlank()) fallbackAnswer(context) else raw.answer
        var isClarification = raw.isClarification
        var clarificationOptions = sanitizeList(raw.clarificationOptions)
        var suggestedNextQuestions = sanitizeList(raw.suggestedNextQuestions)

        if (isClarification && clarificationOptions.isEmpty()) {
            clarificationOptions = deriveClarificationOptions(context)
        }
        if (context.weakGrounding && suggestedNextQuestions.isEmpty()) {
            suggestedNextQuestions = sanitizeList(context.suggestedNextQuestions)
        }
        if (clarificationTurns >= MAX_CLARIFICATION_TURNS && isClarification) {
            isClarification = false
            clarificationOptions = emptyList()
            answer = fallbackAnswer(context)
        }

        answer = enrichAnswer(answer, isClarification, clarificationOptions, context.weakGrounding, suggestedNextQuestions)

        return WikiChatResult(
            answer,
            isClarification,
            clarificationOptions,
            suggestedNextQuestions,
            !topicShift && raw.usedPreviousContext,
            sessionReset,
        )
    }

    private fun normalizeStreamedResult(
        accumulatedAnswer: String,
        chatRequest: ChatRequestContext,
        usedPreviousContext: Boolean,
    ): WikiChatResult =
        normalizeResult(
            WikiChatResult(
                accumulatedAnswer.trim(),
                accumulatedAnswer.contains(CLARIFICATION_HEADING),
                extractBulletList(accumulatedAnswer, CLARIFICATION_HEADING),
                extractBulletList(accumulatedAnswer, SUGGESTED_QUESTION_HEADING),
                usedPreviousContext,
                false,
            ),
            chatRequest.context,
            chatRequest.topicShift,
            chatRequest.sessionReset,
            chatRequest.clarificationTurns,
        )

    private fun fallbackResult(context: WikiRetrievalContext): WikiChatResult =
        WikiChatResult(
            fallbackAnswer(context),
            false,
            emptyList(),
            sanitizeList(context.suggestedNextQuestions),
            false,
            false,
        )

    private fun fallbackAnswer(context: WikiRetrievalContext): String {
        val chunks = context.chunks
        if (!chunks.isNullOrEmpty()) {
            val firstChunk = chunks.first()
            val anchor = firstChunk.sourcePathHint ?: firstChunk.heading
            return if (context.weakGrounding) {
                "요약하면 지금은 $anchor 근거까지만 확인돼요."
            } else {
                "요약하면 핵심 흐름은 $anchor 기준으로 보는 게 가장 안전해요."
            }
        }
        return "요약하면 지금 확보된 저장소 근거만으로는 좁은 범위부터 확인하는 게 안전해요."
    }

    private fun selectPromptTurns(previousTurns: List<ChatTurn>): List<ChatTurn> =
        if (previousTurns.size <= MAX_PROMPT_TURNS) {
            previousTurns
        } else {
            ArrayList(previousTurns.subList(previousTurns.size - MAX_PROMPT_TURNS, previousTurns.size))
        }

    private fun countClarificationTurns(turns: List<ChatTurn>): Int = turns.count { it.wasClarification }

    private fun isTopicShift(
        previousTurns: List<ChatTurn>,
        userQuestion: String,
    ): Boolean {
        if (previousTurns.isEmpty()) {
            return false
        }
        // Meta-history questions reference the conversation itself — never drop context.
        if (META_HISTORY_PATTERN.matcher(userQuestion).find()) {
            return false
        }
        val currentTokens = tokenize(userQuestion)
        // Require >= 2 tokens before judging a topic shift; single-token follow-ups carry too little signal.
        if (currentTokens.size < 2) {
            return false
        }
        val previousTokens = LinkedHashSet<String>()
        for (turn in selectPromptTurns(previousTurns)) {
            previousTokens.addAll(tokenize(turn.question ?: ""))
            previousTokens.addAll(tokenize(turn.answer ?: ""))
        }
        return currentTokens.none { it in previousTokens }
    }

    private fun tokenize(text: String): Set<String> =
        TOKEN_SPLIT
            .split(text.lowercase(Locale.ROOT))
            .filter { it.length >= 2 }
            .toCollection(LinkedHashSet())

    private fun readStringList(node: JsonNode): List<String> {
        if (!node.isArray) {
            return emptyList()
        }
        val values = mutableListOf<String>()
        node.forEach { item ->
            val value = item.asText("").trim()
            if (value.isNotBlank()) {
                values.add(value)
            }
        }
        return values
    }

    private fun sanitizeList(values: List<String>?): List<String> {
        if (values.isNullOrEmpty()) {
            return emptyList()
        }
        val sanitized = LinkedHashSet<String>()
        for (value in values) {
            val trimmed = value.trim()
            if (trimmed.isNotBlank()) {
                sanitized.add(trimmed)
            }
            if (sanitized.size == 3) {
                break
            }
        }
        return sanitized.toList()
    }

    private fun deriveClarificationOptions(context: WikiRetrievalContext): List<String> {
        val options = LinkedHashSet<String>()
        context.chunks?.forEach { chunk ->
            chunk.heading?.let { options.add(it) }
            chunk.sourcePathHint?.let { options.add(it) }
        }
        return sanitizeList(options.toList())
    }

    private fun enrichAnswer(
        answer: String,
        isClarification: Boolean,
        clarificationOptions: List<String>,
        weakGrounding: Boolean,
        suggestedNextQuestions: List<String>,
    ): String {
        if (isClarification) {
            return appendListIfMissing(answer, CLARIFICATION_HEADING, clarificationOptions)
        }
        if (weakGrounding) {
            return appendListIfMissing(answer, SUGGESTED_QUESTION_HEADING, suggestedNextQuestions)
        }
        return answer
    }

    private fun appendListIfMissing(
        answer: String,
        heading: String,
        items: List<String>,
    ): String {
        if (items.isEmpty()) {
            return answer
        }
        if (items.all { answer.contains(it) }) {
            return answer
        }

        val builder = StringBuilder(answer.trim())
        if (builder.isNotEmpty()) {
            builder.append("\n\n")
        }
        builder.append(heading)
        items.forEach { builder.append("\n- ").append(it) }
        return builder.toString()
    }

    private fun extractBulletList(
        answer: String,
        heading: String,
    ): List<String> {
        val headingIndex = answer.indexOf(heading)
        if (headingIndex < 0) {
            return emptyList()
        }

        val remaining = answer.substring(headingIndex + heading.length)
        val items = mutableListOf<String>()
        var readingBullets = false
        for (line in remaining.split(Regex("\\R"))) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (readingBullets) {
                    break
                }
                continue
            }
            if (trimmed.startsWith("-")) {
                items.add(trimmed.substring(1).trim())
                readingBullets = true
                continue
            }
            if (readingBullets) {
                break
            }
        }
        return sanitizeList(items)
    }

    private fun appendStreamChunk(
        chunk: ChatCompletionChunk,
        tokenConsumer: Consumer<String>,
        accumulated: StringBuilder,
    ) {
        for (choice in chunk.choices()) {
            choice.delta().content().ifPresent { token ->
                tokenConsumer.accept(token)
                accumulated.append(token)
            }
        }
    }

    private fun prepareChatRequest(
        sessionId: String,
        projectExternalId: String,
        userQuestion: String,
        user: User?,
    ): ChatRequestContext {
        val context = retrievalService.retrieveContext(projectExternalId, userQuestion)
        val hadActiveSession = sessionStore.hasActiveSession(sessionId)
        val redisTurns = sessionStore.loadRecentTurns(sessionId, projectExternalId)
        // If Redis is cold but the user is authenticated, load from DB (resumed session).
        var previousTurns = redisTurns
        if (redisTurns.isEmpty() && user != null) {
            previousTurns = persistenceService.loadRecentMessages(sessionId, MAX_PROMPT_TURNS)
        }
        val sessionReset = hadActiveSession && redisTurns.isEmpty()
        val topicShift = isTopicShift(previousTurns, userQuestion)
        val promptTurns = if (topicShift) emptyList() else selectPromptTurns(previousTurns)
        val clarificationTurns = countClarificationTurns(previousTurns)
        return ChatRequestContext(context, promptTurns, topicShift, sessionReset, clarificationTurns)
    }

    private fun buildResponseFormat(): ResponseFormatJsonSchema {
        val schema =
            ResponseFormatJsonSchema.JsonSchema.Schema
                .builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty(
                    "properties",
                    JsonValue.from(
                        mapOf(
                            "answer" to mapOf("type" to "string"),
                            "isClarification" to mapOf("type" to "boolean"),
                            "clarificationOptions" to mapOf("type" to "array", "items" to mapOf("type" to "string")),
                            "suggestedNextQuestions" to mapOf("type" to "array", "items" to mapOf("type" to "string")),
                            "usedPreviousContext" to mapOf("type" to "boolean"),
                        ),
                    ),
                ).putAdditionalProperty(
                    "required",
                    JsonValue.from(
                        listOf(
                            "answer",
                            "isClarification",
                            "clarificationOptions",
                            "suggestedNextQuestions",
                            "usedPreviousContext",
                        ),
                    ),
                ).putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()

        return ResponseFormatJsonSchema
            .builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema
                    .builder()
                    .name("wiki_chat_result")
                    .strict(true)
                    .schema(schema)
                    .build(),
            ).build()
    }

    /** Clear session memory explicitly. */
    fun clearSession(sessionId: String) {
        sessionStore.clearSession(sessionId)
    }

    /** True if the session exists and is not expired. */
    fun hasActiveSession(sessionId: String): Boolean = sessionStore.hasActiveSession(sessionId)

    private data class ChatRequestContext(
        val context: WikiRetrievalContext,
        val promptTurns: List<ChatTurn>,
        val topicShift: Boolean,
        val sessionReset: Boolean,
        val clarificationTurns: Int,
    )

    companion object {
        private const val MAX_PROMPT_TURNS = 10
        private const val MAX_CLARIFICATION_TURNS = 2
        private val TOKEN_SPLIT: Pattern = Pattern.compile("[^a-z0-9가-힣]+")
        private const val CLARIFICATION_HEADING = "선택할 수 있는 범위:"
        private const val SUGGESTED_QUESTION_HEADING = "다음처럼 좁혀서 물어보면 더 정확해요:"
        private val META_HISTORY_PATTERN: Pattern =
            Pattern.compile(
                "이전|예전|방금|아까|요약|정리|다시|뭐라고|말했|물어봤|before|previous|summary|summarize|earlier|what did",
                Pattern.CASE_INSENSITIVE,
            )

        private val FAQ_PROBLEM: Pattern =
            Pattern.compile(
                "문제.*해결|해결.*문제|왜 만들|어떤.*목적|what.*problem|solve|why.*built",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_ARCH: Pattern =
            Pattern.compile(
                "아키텍처|구조|설계|architecture|design|어떻게.*동작|동작.*원리|how.*work|내부.*구조",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_START: Pattern =
            Pattern.compile(
                "시작하려면|어떻게.*시작|설치|install|setup|getting.?started|사용.*방법|how.*use|how.*start",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_CHANGES: Pattern =
            Pattern.compile(
                "최근.*변경|변경.*사항|changelog|release|업데이트|최근.*업|새로운|recent.*change|what.*new",
                Pattern.CASE_INSENSITIVE,
            )
        private val FAQ_FEATURES: Pattern =
            Pattern.compile(
                "주요.*기능|기능.*무엇|특징|feature|capabilities|what.*can|뭘.*할|어떤.*기능",
                Pattern.CASE_INSENSITIVE,
            )
    }
}
