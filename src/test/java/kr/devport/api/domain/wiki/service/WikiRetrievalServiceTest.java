package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiRetrievalServiceTest {

    @Mock
    private WikiSectionChunkRepository chunkRepository;

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
        WikiSectionChunk architectureSummary = chunk("architecture", null, "summary", "Architecture overview", "Architecture", "src/main/java/Architecture.java");
        WikiSectionChunk architectureDetails = chunk("architecture", "auth", "body", "Authentication flow details", "Authentication Flow", "src/main/java/AuthFlow.java");
        WikiSectionChunk architectureCache = chunk("architecture", "cache", "body", "Caching details", "Cache Layer", "src/main/java/CacheConfig.java");
        WikiSectionChunk howItWorks = chunk("how-it-works", null, "summary", "Runtime request flow", "How It Works", "src/main/java/RuntimeFlow.java");
        WikiSectionChunk api = chunk("api", null, "summary", "API entrypoints", "Public API", "src/main/java/WikiChatController.java");

        when(chunkRepository.findByProjectExternalId("github:12345"))
                .thenReturn(List.of(architectureSummary, architectureDetails, architectureCache, howItWorks, api));
        when(chunkRepository.findSimilarChunks("github:12345", "[0.12,0.24,0.36]", 8))
                .thenReturn(List.of(architectureSummary, architectureDetails, architectureCache, howItWorks, api));

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
        assertThat(result.groundedContext()).contains("How It Works");
        assertThat(result.suggestedNextQuestions()).isEmpty();
    }

    @Test
    @DisplayName("retrieveContext returns weak-grounding context instead of collapsing to empty context")
    void retrieveContext_returnsWeakGroundingContextOnVectorSearchFailure() {
        WikiSectionChunk chunk = WikiSectionChunk.builder()
                .projectExternalId("github:12345")
                .sectionId("architecture")
                .chunkType("summary")
                .content("JWT filter and refresh handling")
                .metadata(Map.of("titleKo", "인증 흐름", "sourcePath", "src/main/java/.../SecurityConfig.java"))
                .commitSha("abc")
                .build();

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
            String sectionId,
            String subsectionId,
            String chunkType,
            String content,
            String titleKo,
            String sourcePath
    ) {
        return WikiSectionChunk.builder()
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
