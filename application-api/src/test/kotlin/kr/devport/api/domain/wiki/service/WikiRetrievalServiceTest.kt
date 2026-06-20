@file:Suppress("ktlint:standard:max-line-length")

package kr.devport.api.domain.wiki.service

import com.openai.client.OpenAIClient
import com.openai.models.embeddings.Embedding
import com.openai.models.embeddings.EmbeddingCreateParams
import kr.devport.api.domain.wiki.entity.WikiSectionChunk
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepositoryCustom.ScoredChunkRow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WikiRetrievalServiceTest {
    @Mock
    lateinit var chunkRepository: WikiSectionChunkRepository

    @Mock
    lateinit var chunkReranker: WikiChunkReranker

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    lateinit var openAIClient: OpenAIClient

    @InjectMocks
    lateinit var wikiRetrievalService: WikiRetrievalService

    @BeforeEach
    fun setUp() {
        val embedding = mock<Embedding>()
        whenever(embedding.embedding()).thenReturn(listOf(0.12f, 0.24f, 0.36f))
        whenever(openAIClient.embeddings().create(any<EmbeddingCreateParams>()).data()).thenReturn(listOf(embedding))
    }

    @Test
    @DisplayName("retrieveContext throws when project has no wiki chunks")
    fun throwsWhenNoChunks() {
        whenever(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(emptyList())

        assertThatThrownBy { wikiRetrievalService.retrieveContext("github:12345", "How does this work?") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("No wiki content found for project")
    }

    @Test
    @DisplayName("retrieveContext reranks broad candidates to prefer section-diverse grounded chunks")
    fun reranksToPreferSectionDiversity() {
        val architectureSummary =
            chunk(1L, "architecture", null, "summary", "Architecture overview", "Architecture", "src/main/java/Architecture.java")
        val architectureDetails =
            chunk(2L, "architecture", "auth", "body", "Authentication flow details", "Authentication Flow", "src/main/java/AuthFlow.java")
        val architectureCache =
            chunk(3L, "architecture", "cache", "body", "Caching details", "Cache Layer", "src/main/java/CacheConfig.java")
        val howItWorks =
            chunk(4L, "how-it-works", null, "summary", "Runtime request flow", "How It Works", "src/main/java/RuntimeFlow.java")
        val api =
            chunk(5L, "api", null, "summary", "API entrypoints", "Public API", "src/main/java/WikiChatController.java")

        whenever(chunkRepository.findByProjectExternalId("github:12345"))
            .thenReturn(listOf(architectureSummary, architectureDetails, architectureCache, howItWorks, api))
        whenever(chunkRepository.findSimilarChunksWithScore("github:12345", "[0.12,0.24,0.36]", 30))
            .thenReturn(
                listOf(
                    ScoredChunkRow(architectureSummary, 0.82),
                    ScoredChunkRow(architectureDetails, 0.91),
                    ScoredChunkRow(architectureCache, 0.79),
                    ScoredChunkRow(howItWorks, 0.88),
                    ScoredChunkRow(api, 0.84),
                ),
            )
        whenever(chunkRepository.findLexicalCandidates("github:12345", "How is auth wired?", 30))
            .thenReturn(
                listOf(
                    ScoredChunkRow(architectureDetails, 0.73),
                    ScoredChunkRow(api, 0.21),
                ),
            )
        doAnswer { invocation ->
            val input = invocation.getArgument<List<WikiSectionChunk>>(1)
            input.indices.map { index ->
                val score =
                    when (input[index].sectionId) {
                        "architecture" ->
                            if (input[index].subsectionId == "auth") {
                                0.97
                            } else if (input[index].subsectionId == null) {
                                0.80
                            } else {
                                0.74
                            }
                        "how-it-works" -> 0.89
                        "api" -> 0.86
                        else -> 0.50
                    }
                WikiChunkReranker.ScoredChunk(index, score)
            }
        }.whenever(chunkReranker).rerank(eq("How is auth wired?"), any<List<WikiSectionChunk>>())

        val result = wikiRetrievalService.retrieveContext("github:12345", "How is auth wired?")

        assertThat(result.hasGrounding).isTrue()
        assertThat(result.weakGrounding).isFalse()
        assertThat(result.chunks).hasSize(4)
        assertThat(result.chunks!!.map { "${it.sectionId}:${it.heading}" }).containsExactly(
            "architecture:Authentication Flow",
            "how-it-works:How It Works",
            "api:Public API",
            "architecture:Architecture",
        )
        assertThat(result.chunks!!.first().rerankScore).isEqualTo(0.97)
        assertThat(result.groundedContext).contains("How It Works")
        assertThat(result.suggestedNextQuestions).isEmpty()
    }

    @Test
    @DisplayName("retrieveContext returns weak-grounding context when top similarity is below threshold")
    fun returnsWeakGroundingContextWhenSimilarityTooLow() {
        val chunk =
            chunk(1L, "architecture", null, "summary", "JWT filter and refresh handling", "인증 흐름", "src/main/java/.../SecurityConfig.java")

        whenever(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(listOf(chunk))
        whenever(chunkRepository.findSimilarChunksWithScore("github:12345", "[0.12,0.24,0.36]", 30))
            .thenReturn(listOf(ScoredChunkRow(chunk, 0.18)))
        whenever(chunkRepository.findLexicalCandidates("github:12345", "How does auth work?", 30))
            .thenReturn(emptyList())

        val result = wikiRetrievalService.retrieveContext("github:12345", "How does auth work?")

        assertThat(result.hasGrounding).isTrue()
        assertThat(result.weakGrounding).isTrue()
        assertThat(result.groundedContext).contains("인증 흐름")
        assertThat(result.chunks!!.single().heading).isEqualTo("인증 흐름")
        assertThat(result.suggestedNextQuestions).hasSizeBetween(2, 3)
    }

    @Test
    @DisplayName("retrieveContext returns weak-grounding context instead of collapsing to empty context on retrieval failure")
    fun returnsWeakGroundingContextOnVectorSearchFailure() {
        val chunk =
            chunk(1L, "architecture", null, "summary", "JWT filter and refresh handling", "인증 흐름", "src/main/java/.../SecurityConfig.java")

        whenever(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(listOf(chunk))
        whenever(openAIClient.embeddings().create(any<EmbeddingCreateParams>()))
            .thenThrow(RuntimeException("embedding outage"))

        val result = wikiRetrievalService.retrieveContext("github:12345", "How does auth work?")

        assertThat(result.hasGrounding).isTrue()
        assertThat(result.weakGrounding).isTrue()
        assertThat(result.groundedContext).contains("인증 흐름")
        assertThat(result.chunks!!.single().heading).isEqualTo("인증 흐름")
        assertThat(result.suggestedNextQuestions).hasSizeBetween(2, 3)
    }

    @Test
    @DisplayName("retrieveContext skips reranker when top match is already clearly strong")
    fun skipsRerankerWhenTopMatchIsClear() {
        val auth =
            chunk(1L, "architecture", "auth", "body", "Authentication flow details", "Authentication Flow", "src/main/java/AuthFlow.java")
        val api = chunk(2L, "api", null, "summary", "API entrypoints", "Public API", "src/main/java/WikiChatController.java")

        whenever(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(listOf(auth, api))
        whenever(chunkRepository.findSimilarChunksWithScore("github:12345", "[0.12,0.24,0.36]", 30))
            .thenReturn(
                listOf(
                    ScoredChunkRow(auth, 0.92),
                    ScoredChunkRow(api, 0.79),
                ),
            )
        whenever(chunkRepository.findLexicalCandidates("github:12345", "How is auth wired?", 30))
            .thenReturn(
                listOf(
                    ScoredChunkRow(auth, 0.61),
                    ScoredChunkRow(api, 0.20),
                ),
            )

        val result = wikiRetrievalService.retrieveContext("github:12345", "How is auth wired?")

        assertThat(result.hasGrounding).isTrue()
        assertThat(result.weakGrounding).isFalse()
        verify(chunkReranker, never()).rerank(any(), any())
        assertThat(result.chunks!!.first().heading).isEqualTo("Authentication Flow")
        assertThat(result.chunks!!.first().rerankScore).isNull()
    }

    private fun chunk(
        id: Long,
        sectionId: String,
        subsectionId: String?,
        chunkType: String,
        content: String,
        titleKo: String,
        sourcePath: String,
    ): WikiSectionChunk =
        WikiSectionChunk().apply {
            this.id = id
            projectExternalId = "github:12345"
            this.sectionId = sectionId
            this.subsectionId = subsectionId
            this.chunkType = chunkType
            this.content = content
            metadata = mutableMapOf("titleKo" to titleKo, "sourcePath" to sourcePath)
            commitSha = "abc"
        }
}
