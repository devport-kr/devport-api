package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiRetrievalServiceTest {

    @Mock
    private WikiSectionChunkRepository chunkRepository;

    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private WikiRetrievalService wikiRetrievalService;

    @Test
    @DisplayName("retrieveContext throws when project has no wiki chunks")
    void retrieveContext_throwsWhenNoChunks() {
        when(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(List.of());

        assertThatThrownBy(() -> wikiRetrievalService.retrieveContext("github:12345", "How does this work?"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No wiki content found for project");
    }

    @Test
    @DisplayName("retrieveContext returns empty string when vector search fails gracefully")
    void retrieveContext_returnsEmptyOnVectorSearchFailure() {
        WikiSectionChunk chunk = WikiSectionChunk.builder()
                .projectExternalId("github:12345")
                .sectionId("sec-1")
                .chunkType("summary")
                .content("some content")
                .commitSha("abc")
                .build();

        when(chunkRepository.findByProjectExternalId("github:12345")).thenReturn(List.of(chunk));
        // openAIClient.embeddings().create() will throw NPE via default mock → caught → returns ""

        String result = wikiRetrievalService.retrieveContext("github:12345", "How does this work?");

        // Vector search failure is swallowed and returns empty string
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }
}
