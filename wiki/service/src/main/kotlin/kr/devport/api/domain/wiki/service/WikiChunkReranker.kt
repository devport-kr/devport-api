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
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WikiChunkReranker(
    private val openAIClient: OpenAIClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun rerank(
        question: String,
        candidates: List<WikiSectionChunk>,
    ): List<ScoredChunk> {
        if (candidates.isEmpty()) {
            return emptyList()
        }

        val cappedCandidates = candidates.take(MAX_CANDIDATES)

        val completion =
            openAIClient.chat().completions().create(
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.GPT_5_MINI)
                    .messages(buildMessages(question, cappedCandidates))
                    .responseFormat(buildResponseFormat())
                    .build(),
            )

        val payload = completion.choices().first().message().content().orElse("")
        val parsed = parse(payload)
        return if (parsed.isEmpty()) fallbackScores(cappedCandidates.size) else parsed
    }

    private fun buildMessages(
        question: String,
        candidates: List<WikiSectionChunk>,
    ): List<ChatCompletionMessageParam> =
        listOf(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam
                    .builder()
                    .content(
                        """
                        You are reranking repository wiki chunks for retrieval.
                        Return JSON only.
                        Score chunks by how directly they help answer the question.
                        Higher score means more useful grounding.
                        Prefer chunks with exact technical relevance over broad summaries.
                        """.trimIndent(),
                    ).build(),
            ),
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam
                    .builder()
                    .content(buildUserPrompt(question, candidates))
                    .build(),
            ),
        )

    private fun buildUserPrompt(
        question: String,
        candidates: List<WikiSectionChunk>,
    ): String {
        val prompt = StringBuilder()
        prompt.append("Question:\n").append(question).append("\n\nCandidates:\n")
        candidates.forEachIndexed { i, chunk ->
            prompt.append("Index: ").append(i).append("\n")
            prompt.append("Heading: ").append(resolveHeading(chunk)).append("\n")
            prompt.append("Content:\n").append(truncate(chunk.content)).append("\n\n")
        }
        prompt.append(
            """
            Return all candidates in a `scores` array.
            Each item must include:
            - index
            - score
            """.trimIndent(),
        )
        return prompt.toString()
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
                            "scores" to
                                mapOf(
                                    "type" to "array",
                                    "items" to
                                        mapOf(
                                            "type" to "object",
                                            "properties" to
                                                mapOf(
                                                    "index" to mapOf("type" to "integer"),
                                                    "score" to mapOf("type" to "number"),
                                                ),
                                            "required" to listOf("index", "score"),
                                            "additionalProperties" to false,
                                        ),
                                ),
                        ),
                    ),
                ).putAdditionalProperty("required", JsonValue.from(listOf("scores")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()

        return ResponseFormatJsonSchema
            .builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema
                    .builder()
                    .name("wiki_chunk_rerank_result")
                    .strict(true)
                    .schema(schema)
                    .build(),
            ).build()
    }

    private fun parse(payload: String): List<ScoredChunk> =
        try {
            val root = objectMapper.readTree(payload)
            val scores = root.path("scores")
            if (!scores.isArray) {
                emptyList()
            } else {
                val parsed = mutableListOf<ScoredChunk>()
                scores.forEach { node ->
                    if (node.has("index") && node.has("score")) {
                        parsed.add(ScoredChunk(node.path("index").asInt(-1), node.path("score").asDouble(0.0)))
                    }
                }
                parsed.filter { it.index >= 0 }.sortedByDescending { it.score }
            }
        } catch (e: Exception) {
            log.warn("wiki-reranker: failed to parse reranker output: {}", e.message)
            emptyList()
        }

    private fun fallbackScores(size: Int): List<ScoredChunk> = (0 until size).map { i -> ScoredChunk(i, (size - i).toDouble()) }

    private fun truncate(content: String?): String {
        val c = content ?: ""
        return if (c.length <= MAX_CONTENT_CHARS) c else c.substring(0, MAX_CONTENT_CHARS) + "..."
    }

    private fun resolveHeading(chunk: WikiSectionChunk): String? {
        chunk.metadata?.get("titleKo")?.let { titleKo ->
            if (titleKo.toString().isNotBlank()) {
                return titleKo.toString()
            }
        }
        val sub = chunk.subsectionId
        if (!sub.isNullOrBlank()) {
            return "${chunk.sectionId} > $sub"
        }
        return chunk.sectionId
    }

    data class ScoredChunk(
        val index: Int,
        val score: Double,
    )

    companion object {
        private const val MAX_CANDIDATES = 20
        private const val MAX_CONTENT_CHARS = 600
    }
}
