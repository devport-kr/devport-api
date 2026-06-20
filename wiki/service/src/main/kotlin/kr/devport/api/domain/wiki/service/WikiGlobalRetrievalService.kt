package kr.devport.api.domain.wiki.service

import com.openai.client.OpenAIClient
import com.openai.models.embeddings.EmbeddingCreateParams
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext.ScoredProject
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Cross-project RAG retrieval for global wiki chat: finds the most relevant projects across all
 * wiki content for a question.
 */
@Service
class WikiGlobalRetrievalService(
    private val chunkRepository: WikiSectionChunkRepository,
    private val openAIClient: OpenAIClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun retrieve(question: String): WikiGlobalRetrievalContext =
        try {
            val vectorStr = toVectorString(embedText(question))
            val topChunks = chunkRepository.findSimilarChunksGlobalWithScore(vectorStr, GLOBAL_CANDIDATE_LIMIT)

            if (topChunks.isEmpty()) {
                WikiGlobalRetrievalContext("", emptyList(), false)
            } else {
                val byProject = LinkedHashMap<String, MutableList<IndexedChunk>>()
                for (row in topChunks) {
                    byProject
                        .getOrPut(row.chunk.projectExternalId!!) { mutableListOf() }
                        .add(IndexedChunk(row.chunk, row.score))
                }

                val scoredProjects =
                    byProject.entries
                        .map { entry ->
                            val chunks = entry.value
                            val projectScore =
                                chunks
                                    .sortedByDescending { it.similarity }
                                    .take(2)
                                    .sumOf { it.similarity }
                            val bestChunks =
                                chunks
                                    .sortedByDescending { it.similarity }
                                    .take(CHUNKS_PER_PROJECT)
                                    .map { ic ->
                                        WikiRetrievedChunk(
                                            sectionId = ic.chunk.sectionId,
                                            subsectionId = ic.chunk.subsectionId,
                                            chunkType = ic.chunk.chunkType,
                                            heading = resolveHeading(ic.chunk),
                                            content = ic.chunk.content,
                                            similarityScore = ic.similarity,
                                            headingScore = 0.0,
                                            rerankScore = null,
                                            sourcePathHint = null,
                                        )
                                    }
                            ScoredProject(entry.key, projectScore, bestChunks)
                        }.sortedByDescending { it.score }
                        .take(MAX_PROJECTS)

                WikiGlobalRetrievalContext(buildContext(scoredProjects), scoredProjects, scoredProjects.isNotEmpty())
            }
        } catch (e: Exception) {
            log.warn("wiki-global-retrieval: retrieval failed: {}", e.message)
            WikiGlobalRetrievalContext("", emptyList(), false)
        }

    private fun buildContext(scoredProjects: List<ScoredProject>): String {
        val sb = StringBuilder("# Multi-Project Context\n\n")
        for (project in scoredProjects) {
            sb.append("## Project: ").append(project.projectExternalId).append("\n\n")
            project.topChunks?.forEach { chunk ->
                sb.append("### ").append(chunk.heading).append("\n\n")
                sb.append(chunk.content).append("\n\n")
            }
        }
        return truncateToTokenLimit(sb.toString(), MAX_CONTEXT_TOKENS)
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

    private fun embedText(text: String): FloatArray {
        val params =
            EmbeddingCreateParams
                .builder()
                .model("text-embedding-3-small")
                .input(text)
                .build()
        val response = openAIClient.embeddings().create(params)
        val embeddingFloats = response.data().first().embedding()
        return FloatArray(embeddingFloats.size) { embeddingFloats[it] }
    }

    private fun toVectorString(vector: FloatArray): String = vector.joinToString(",", prefix = "[", postfix = "]")

    private fun truncateToTokenLimit(
        text: String,
        maxTokens: Int,
    ): String {
        val maxChars = maxTokens * 4
        return if (text.length <= maxChars) text else text.substring(0, maxChars) + "..."
    }

    private data class IndexedChunk(
        val chunk: WikiSectionChunk,
        val similarity: Double,
    )

    companion object {
        private const val GLOBAL_CANDIDATE_LIMIT = 20
        private const val MAX_PROJECTS = 5
        private const val CHUNKS_PER_PROJECT = 2
        private const val MAX_CONTEXT_TOKENS = 4000
    }
}
