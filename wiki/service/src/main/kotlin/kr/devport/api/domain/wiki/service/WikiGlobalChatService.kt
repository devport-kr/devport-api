package kr.devport.api.domain.wiki.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.devport.api.domain.port.infrastructure.ProjectDirectory
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult.RelatedProjectLlmOutput
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext
import kr.devport.api.domain.wiki.dto.response.RelatedProjectResponse
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse
import kr.devport.api.domain.wiki.enums.WikiChatSessionType
import kr.devport.api.domain.wiki.infrastructure.ChatMessage
import kr.devport.api.domain.wiki.infrastructure.ChatPort
import kr.devport.api.domain.wiki.infrastructure.ChatRole
import kr.devport.api.domain.wiki.infrastructure.ChatTurn
import kr.devport.api.domain.wiki.infrastructure.JsonSchemaSpec
import kr.devport.api.domain.wiki.infrastructure.WikiChatSessionStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.function.Consumer

/**
 * Global wiki chat discovers relevant projects across all wikis. The core owns prompt, parsing, and
 * persistence policy; the LLM and project lookup details live behind ports.
 */
@Service
class WikiGlobalChatService(
    private val retrievalService: WikiGlobalRetrievalService,
    private val sessionStore: WikiChatSessionStore,
    private val persistenceService: WikiChatSessionPersistenceService,
    private val titleService: WikiChatTitleService,
    private val projectDirectory: ProjectDirectory,
    private val chatPort: ChatPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun chatResult(
        sessionId: String,
        question: String,
        userId: Long?,
    ): WikiGlobalChatResponse {
        val context = retrievalService.retrieve(question)
        val promptTurns = loadContextTurns(sessionId, userId)
        val payload =
            chatPort.complete(
                model = CHAT_MODEL,
                messages = buildMessages(context, promptTurns, question),
                jsonSchema = buildResponseFormat(),
            )

        val result = parseResult(payload)
        val answer = result.answer ?: ""

        persist(sessionId, question, answer, userId)
        sessionStore.saveTurn(sessionId, null, question, answer, false)

        val enriched = enrichProjects(result.relatedProjects ?: emptyList())
        return WikiGlobalChatResponse(result.answer, enriched, result.hasRelatedProjects, sessionId)
    }

    fun streamChatResult(
        sessionId: String,
        question: String,
        tokenConsumer: Consumer<String>,
        userId: Long?,
    ): WikiGlobalChatResponse {
        val context = retrievalService.retrieve(question)
        val promptTurns = loadContextTurns(sessionId, userId)
        val accumulated = StringBuilder()

        chatPort.stream(CHAT_MODEL, buildStreamMessages(context, promptTurns, question)) { token ->
            tokenConsumer.accept(token)
            accumulated.append(token)
        }

        val answer = accumulated.toString().trim()
        persist(sessionId, question, answer, userId)
        sessionStore.saveTurn(sessionId, null, question, answer, false)

        val relatedFromContext =
            (context.scoredProjects ?: emptyList()).map { sp ->
                RelatedProjectLlmOutput(sp.projectExternalId, "")
            }
        val enriched = enrichProjects(relatedFromContext)
        return WikiGlobalChatResponse(answer, enriched, enriched.isNotEmpty(), sessionId)
    }

    private fun persist(
        sessionId: String,
        question: String,
        answer: String,
        userId: Long?,
    ) {
        if (userId == null) {
            return
        }

        val isFirst = persistenceService.isFirstMessage(sessionId)
        val session = persistenceService.findOrCreateSession(sessionId, userId, null, WikiChatSessionType.GLOBAL)
        persistenceService.saveUserMessage(session, question)
        persistenceService.saveAssistantMessage(session, answer, false)
        if (isFirst) {
            titleService.generateAndSave(sessionId, question)
        }
    }

    private fun loadContextTurns(
        sessionId: String,
        userId: Long?,
    ): List<ChatTurn> {
        val redisTurns = sessionStore.loadRecentTurns(sessionId, null)
        if (redisTurns.isNotEmpty()) {
            return redisTurns
        }
        if (userId != null) {
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
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(ChatRole.SYSTEM, buildSystemPrompt(context)))
        for (turn in previousTurns) {
            messages.add(ChatMessage(ChatRole.USER, turn.question ?: ""))
            // Global chat replays prior answers as USER turns (preserves original behavior).
            messages.add(ChatMessage(ChatRole.USER, turn.answer ?: ""))
        }
        messages.add(ChatMessage(ChatRole.USER, "질문: $question\n\nJSON으로만 응답하세요."))
        return messages
    }

    private fun buildStreamMessages(
        context: WikiGlobalRetrievalContext,
        previousTurns: List<ChatTurn>,
        question: String,
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(ChatRole.SYSTEM, buildStreamSystemPrompt(context)))
        for (turn in previousTurns) {
            messages.add(ChatMessage(ChatRole.USER, turn.question ?: ""))
            // Global chat replays prior answers as USER turns (preserves original behavior).
            messages.add(ChatMessage(ChatRole.USER, turn.answer ?: ""))
        }
        messages.add(ChatMessage(ChatRole.USER, "질문: $question"))
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
        Be honest - do not invent relevance.

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
                val project = projectDirectory.findByExternalId(llmOutput.projectExternalId ?: "") ?: return@mapNotNull null
                RelatedProjectResponse(
                    projectExternalId = project.externalId,
                    fullName = project.fullName,
                    description = project.description,
                    relevanceReason = llmOutput.relevanceReason,
                    stars = project.stars ?: 0,
                )
            } catch (e: Exception) {
                log.warn("wiki-global-chat: Failed to enrich project {}: {}", llmOutput.projectExternalId, e.message)
                null
            }
        }

    private fun buildResponseFormat(): JsonSchemaSpec =
        JsonSchemaSpec(
            name = "wiki_global_chat_result",
            schema =
                mapOf(
                    "type" to "object",
                    "properties" to
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
                    "required" to listOf("answer", "relatedProjects", "hasRelatedProjects"),
                    "additionalProperties" to false,
                ),
        )

    companion object {
        private const val MAX_PROMPT_TURNS = 10
        private const val CHAT_MODEL = "gpt-4o-mini"
    }
}
