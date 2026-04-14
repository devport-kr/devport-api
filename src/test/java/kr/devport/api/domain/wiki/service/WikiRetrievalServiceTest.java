package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepositoryCustom.ScoredChunkRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiRetrievalServiceTest {

    @Mock
    private WikiSectionChunkRepository chunkRepository;

    @Mock
    private WikiChunkReranker chunkReranker;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient openAIClient;

    @InjectMocks
    private WikiRetrievalService wikiRetrievalService;

    @BeforeEach
    void setUp() {
        when(openAIClient.embeddings().create(any(EmbeddingCreateParams.class)).data().getFirst().embedding())
                .thenReturn(List.of(0.12f, 0.24f, 0.36f));
    }

    @Test
    @DisplayName("retrieveContext throws when project has no wiki chunks")
    void retrieveContext_throwsWhenNoChunks() {
        when(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(List.of());

        assertThatThrownBy(() -> wikiRetrievalService.retrieveContext("github:12345", "How does this work?"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No wiki content found for project");
    }

    @Test
    @DisplayName("retrieveContext reranks broad candidates to prefer section-diverse grounded chunks")
    void retrieveContext_reranksToPreferSectionDiversity() {
        WikiSectionChunk architectureSummary = chunk(1L, "architecture", null, "summary", "Architecture overview", "Architecture", "src/main/java/Architecture.java");
        WikiSectionChunk architectureDetails = chunk(2L, "architecture", "auth", "body", "Authentication flow details", "Authentication Flow", "src/main/java/AuthFlow.java");
        WikiSectionChunk architectureCache = chunk(3L, "architecture", "cache", "body", "Caching details", "Cache Layer", "src/main/java/CacheConfig.java");
        WikiSectionChunk howItWorks = chunk(4L, "how-it-works", null, "summary", "Runtime request flow", "How It Works", "src/main/java/RuntimeFlow.java");
        WikiSectionChunk api = chunk(5L, "api", null, "summary", "API entrypoints", "Public API", "src/main/java/WikiChatController.java");

        when(chunkRepository.findByProjectExternalId("github:12345"))
                .thenReturn(List.of(architectureSummary, architectureDetails, architectureCache, howItWorks, api));
        when(chunkRepository.findSimilarChunksWithScore("github:12345", "[0.12,0.24,0.36]", 30))
                .thenReturn(List.of(
                        new ScoredChunkRow(architectureSummary, 0.82d),
                        new ScoredChunkRow(architectureDetails, 0.91d),
                        new ScoredChunkRow(architectureCache, 0.79d),
                        new ScoredChunkRow(howItWorks, 0.88d),
                        new ScoredChunkRow(api, 0.84d)
                ));
        when(chunkRepository.findLexicalCandidates("github:12345", "How is auth wired?", 30))
                .thenReturn(List.of(
                        new ScoredChunkRow(architectureDetails, 0.73d),
                        new ScoredChunkRow(api, 0.21d)
                ));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<WikiSectionChunk> input = invocation.getArgument(1);
            return IntStream.range(0, input.size())
                    .mapToObj(index -> new WikiChunkReranker.ScoredChunk(index, switch (input.get(index).getSectionId()) {
                        case "architecture" -> "auth".equals(input.get(index).getSubsectionId()) ? 0.97d
                                : input.get(index).getSubsectionId() == null ? 0.80d : 0.74d;
                        case "how-it-works" -> 0.89d;
                        case "api" -> 0.86d;
                        default -> 0.50d;
                    }))
                    .toList();
        }).when(chunkReranker).rerank(eq("How is auth wired?"), anyList());

        WikiRetrievalContext result = wikiRetrievalService.retrieveContext("github:12345", "How is auth wired?");

        assertThat(result.hasGrounding()).isTrue();
        assertThat(result.weakGrounding()).isFalse();
        assertThat(result.chunks()).hasSize(4);
        assertThat(result.chunks())
                .extracting(chunk -> chunk.sectionId() + ":" + chunk.heading())
                .containsExactly(
                        "architecture:Authentication Flow",
                        "how-it-works:How It Works",
                        "api:Public API",
                        "architecture:Architecture"
                );
        assertThat(result.chunks().getFirst().rerankScore()).isEqualTo(0.97d);
        assertThat(result.groundedContext()).contains("How It Works");
        assertThat(result.suggestedNextQuestions()).isEmpty();
    }

    @Test
    @DisplayName("retrieveContext returns weak-grounding context when top similarity is below threshold")
    void retrieveContext_returnsWeakGroundingContextWhenSimilarityTooLow() {
        WikiSectionChunk chunk = chunk(1L, "architecture", null, "summary", "JWT filter and refresh handling", "인증 흐름", "src/main/java/.../SecurityConfig.java");

        when(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(List.of(chunk));
        when(chunkRepository.findSimilarChunksWithScore("github:12345", "[0.12,0.24,0.36]", 30))
                .thenReturn(List.of(new ScoredChunkRow(chunk, 0.18d)));
        when(chunkRepository.findLexicalCandidates("github:12345", "How does auth work?", 30))
                .thenReturn(List.of());

        WikiRetrievalContext result = wikiRetrievalService.retrieveContext("github:12345", "How does auth work?");

        assertThat(result.hasGrounding()).isTrue();
        assertThat(result.weakGrounding()).isTrue();
        assertThat(result.groundedContext()).contains("인증 흐름");
        assertThat(result.chunks()).singleElement().extracting(chunkResult -> chunkResult.heading())
                .isEqualTo("인증 흐름");
        assertThat(result.suggestedNextQuestions()).hasSizeBetween(2, 3);
    }

    @Test
    @DisplayName("retrieveContext returns weak-grounding context instead of collapsing to empty context on retrieval failure")
    void retrieveContext_returnsWeakGroundingContextOnVectorSearchFailure() {
        WikiSectionChunk chunk = chunk(1L, "architecture", null, "summary", "JWT filter and refresh handling", "인증 흐름", "src/main/java/.../SecurityConfig.java");

        when(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(List.of(chunk));
        when(openAIClient.embeddings().create(any(EmbeddingCreateParams.class)))
                .thenThrow(new RuntimeException("embedding outage"));

        WikiRetrievalContext result = wikiRetrievalService.retrieveContext("github:12345", "How does auth work?");

        assertThat(result.hasGrounding()).isTrue();
        assertThat(result.weakGrounding()).isTrue();
        assertThat(result.groundedContext()).contains("인증 흐름");
        assertThat(result.chunks()).singleElement().extracting(chunkResult -> chunkResult.heading())
                .isEqualTo("인증 흐름");
        assertThat(result.suggestedNextQuestions()).hasSizeBetween(2, 3);
    }

    private WikiSectionChunk chunk(
            Long id,
            String sectionId,
            String subsectionId,
            String chunkType,
            String content,
            String titleKo,
            String sourcePath
    ) {
        return WikiSectionChunk.builder()
                .id(id)
                .projectExternalId("github:12345")
                .sectionId(sectionId)
                .subsectionId(subsectionId)
                .chunkType(chunkType)
                .content(content)
                .metadata(Map.of("titleKo", titleKo, "sourcePath", sourcePath))
                .commitSha("abc")
                .build();
    }
}
