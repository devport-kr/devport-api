package kr.devport.api.domain.wiki.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.client.OpenAIClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.port.repository.ProjectRepository
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult.RelatedProjectLlmOutput
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext
import kr.devport.api.domain.wiki.dto.response.RelatedProjectResponse
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import kr.devport.api.domain.wiki.store.WikiChatSessionStore
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.function.Consumer

/**
 * Global wiki chat — discovers relevant projects across all wikis. Returns an answer plus a related
 * project list (structured JSON for the non-streaming path, retrieval-derived for streaming).
 */
@Service
class WikiGlobalChatService(
    private val retrievalService: WikiGlobalRetrievalService,
    private val sessionStore: WikiChatSessionStore,
    private val persistenceService: WikiChatSessionPersistenceService,
    private val titleService: WikiChatTitleService,
    private val projectRepository: ProjectRepository,
    private val openAIClient: OpenAIClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun chatResult(
        sessionId: String,
        question: String,
        user: User?,
    ): WikiGlobalChatResponse {
        val context = retrievalService.retrieve(question)
        val promptTurns = loadContextTurns(sessionId, user)
        val messages = buildMessages(context, promptTurns, question)

        val completion =
            openAIClient.chat().completions().create(
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .messages(messages)
                    .responseFormat(buildResponseFormat())
                    .build(),
            )

        val payload = completion.choices().first().message().content().orElse("")
        val result = parseResult(payload)
        val answer = result.answer ?: ""

        val isFirst = persistenceService.isFirstMessage(sessionId)
        if (user != null) {
            val session = persistenceService.findOrCreateSession(sessionId, user, null, WikiChatSessionType.GLOBAL)
            persistenceService.saveUserMessage(session, question)
            persistenceService.saveAssistantMessage(session, answer, false)
            if (isFirst) {
                titleService.generateAndSave(sessionId, question)
            }
        }
        sessionStore.saveTurn(sessionId, null, question, answer, false)

        val enriched = enrichProjects(result.relatedProjects ?: emptyList())
        return WikiGlobalChatResponse(result.answer, enriched, result.hasRelatedProjects, sessionId)
    }

    fun streamChatResult(
        sessionId: String,
        question: String,
        tokenConsumer: Consumer<String>,
        user: User?,
    ): WikiGlobalChatResponse {
        val context = retrievalService.retrieve(question)
        val promptTurns = loadContextTurns(sessionId, user)
        val messages = buildStreamMessages(context, promptTurns, question)

        val accumulated = StringBuilder()

        openAIClient
            .chat()
            .completions()
            .createStreaming(
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .messages(messages)
                    .build(),
            ).use { completionStream ->
                completionStream.stream().use { chunks ->
                    chunks.forEach { chunk ->
                        for (choice in chunk.choices()) {
                            choice.delta().content().ifPresent { token ->
                                tokenConsumer.accept(token)
                                accumulated.append(token)
                            }
                        }
                    }
                }
            }

        val answer = accumulated.toString().trim()

        val isFirst = persistenceService.isFirstMessage(sessionId)
        if (user != null) {
            val session = persistenceService.findOrCreateSession(sessionId, user, null, WikiChatSessionType.GLOBAL)
            persistenceService.saveUserMessage(session, question)
            persistenceService.saveAssistantMessage(session, answer, false)
            if (isFirst) {
                titleService.generateAndSave(sessionId, question)
            }
        }
        sessionStore.saveTurn(sessionId, null, question, answer, false)

        // For streaming, related projects come from the retrieval context (no JSON parsing).
        val relatedFromContext =
            (context.scoredProjects ?: emptyList()).map { sp ->
                RelatedProjectLlmOutput(sp.projectExternalId, "")
            }
        val enriched = enrichProjects(relatedFromContext)
        return WikiGlobalChatResponse(answer, enriched, enriched.isNotEmpty(), sessionId)
    }

    private fun loadContextTurns(
        sessionId: String,
        user: User?,
    ): List<ChatTurn> {
        val redisTurns = sessionStore.loadRecentTurns(sessionId, null)
        if (redisTurns.isNotEmpty()) {
            return redisTurns
        }
        if (user != null) {
            val dbTurns = persistenceService.loadRecentMessages(sessionId, MAX_PROMPT_TURNS)
            if (dbTurns.isNotEmpty()) {
                return dbTurns
            }
        }
        return emptyList()
    }

    private fun buildMessages(
        context: WikiGlobalRetrievalContext,
        previousTurns: List<ChatTurn>,
        question: String,
    ): List<ChatCompletionMessageParam> {
        val messages = mutableListOf<ChatCompletionMessageParam>()
        messages.add(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(buildSystemPrompt(context)).build(),
            ),
        )
        for (turn in previousTurns) {
            messages.add(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.question ?: "").build(),
                ),
            )
            messages.add(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.answer ?: "").build(),
                ),
            )
        }
        messages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content("질문: $question\n\nJSON으로만 응답하세요.").build(),
            ),
        )
        return messages
    }

    private fun buildStreamMessages(
        context: WikiGlobalRetrievalContext,
        previousTurns: List<ChatTurn>,
        question: String,
    ): List<ChatCompletionMessageParam> {
        val messages = mutableListOf<ChatCompletionMessageParam>()
        messages.add(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(buildStreamSystemPrompt(context)).build(),
            ),
        )
        for (turn in previousTurns) {
            messages.add(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.question ?: "").build(),
                ),
            )
            messages.add(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.answer ?: "").build(),
                ),
            )
        }
        messages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content("질문: $question").build(),
            ),
        )
        return messages
    }

    private fun buildSystemPrompt(context: WikiGlobalRetrievalContext): String =
        """
        You are a developer tool that helps users find relevant open-source projects.
        Given context from multiple project wikis, identify which projects best match the user's query.
        Always write the answer in Korean only.

        Return JSON with this exact schema:
        {
          "answer": "1-2 sentence conversational response in Korean",
          "relatedProjects": [
            { "projectExternalId": "...", "relevanceReason": "Why this project matches (1 sentence in Korean)" }
          ],
          "hasRelatedProjects": true/false
        }

        If no projects are truly relevant, return an empty relatedProjects array and set hasRelatedProjects to false.
        Be honest — do not invent relevance.

        Multi-Project Context:
        ${context.groundedContext}
        """.trimIndent()

    private fun buildStreamSystemPrompt(context: WikiGlobalRetrievalContext): String =
        """
        You are a developer tool that helps users find relevant open-source projects.
        Given context from multiple project wikis, answer the user's question concisely in Korean.
        Be conversational and helpful. Focus on which projects are most relevant and why.

        Multi-Project Context:
        ${context.groundedContext}
        """.trimIndent()

    private fun parseResult(payload: String): WikiGlobalChatResult =
        try {
            val root = objectMapper.readTree(payload)
            val answer = root.path("answer").asText("").trim()
            val hasRelated = root.path("hasRelatedProjects").asBoolean(false)
            val relatedProjects = mutableListOf<RelatedProjectLlmOutput>()
            val arr = root.path("relatedProjects")
            if (arr.isArray) {
                arr.forEach { node ->
                    val projectId = node.path("projectExternalId").asText("").trim()
                    val reason = node.path("relevanceReason").asText("").trim()
                    if (projectId.isNotBlank()) {
                        relatedProjects.add(RelatedProjectLlmOutput(projectId, reason))
                    }
                }
            }
            WikiGlobalChatResult(answer, relatedProjects, hasRelated, false)
        } catch (e: Exception) {
            log.warn("wiki-global-chat: Failed to parse LLM response: {}", e.message)
            WikiGlobalChatResult("죄송합니다, 응답을 처리하는 중 문제가 발생했습니다.", emptyList(), false, false)
        }

    private fun enrichProjects(llmOutputs: List<RelatedProjectLlmOutput>): List<RelatedProjectResponse> =
        llmOutputs.mapNotNull { llmOutput ->
            try {
                projectRepository
                    .findByExternalId(llmOutput.projectExternalId ?: "")
                    .map { project ->
                        RelatedProjectResponse(
                            projectExternalId = project.externalId,
                            fullName = project.fullName,
                            description = project.description,
                            relevanceReason = llmOutput.relevanceReason,
                            stars = project.stars ?: 0,
                        )
                    }.orElse(null)
            } catch (e: Exception) {
                log.warn("wiki-global-chat: Failed to enrich project {}: {}", llmOutput.projectExternalId, e.message)
                null
            }
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
                            "relatedProjects" to
                                mapOf(
                                    "type" to "array",
                                    "items" to
                                        mapOf(
                                            "type" to "object",
                                            "properties" to
                                                mapOf(
                                                    "projectExternalId" to mapOf("type" to "string"),
                                                    "relevanceReason" to mapOf("type" to "string"),
                                                ),
                                            "required" to listOf("projectExternalId", "relevanceReason"),
                                            "additionalProperties" to false,
                                        ),
                                ),
                            "hasRelatedProjects" to mapOf("type" to "boolean"),
                        ),
                    ),
                ).putAdditionalProperty(
                    "required",
                    JsonValue.from(listOf("answer", "relatedProjects", "hasRelatedProjects")),
                ).putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()

        return ResponseFormatJsonSchema
            .builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema
                    .builder()
                    .name("wiki_global_chat_result")
                    .strict(true)
                    .schema(schema)
                    .build(),
            ).build()
    }

    companion object {
        private const val MAX_PROMPT_TURNS = 10
    }
}
