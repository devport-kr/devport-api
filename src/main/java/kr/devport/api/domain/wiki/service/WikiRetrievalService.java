package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wiki chat retrieval service using vector similarity search over wiki_section_chunks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiRetrievalService {

    private final WikiSectionChunkRepository chunkRepository;
    private final OpenAIClient openAIClient;

    /**
     * Retrieve grounded context for a chat turn via vector similarity search.
     * Throws if no chunks exist for this project.
     */
    public String retrieveContext(String projectExternalId, String userQuestion) {
        List<WikiSectionChunk> allChunks = chunkRepository.findByProjectExternalId(projectExternalId);
        if (allChunks.isEmpty()) {
            throw new IllegalArgumentException("No wiki content found for project: " + projectExternalId);
        }

        try {
            float[] questionEmbedding = embedText(userQuestion);
            String vectorStr = toVectorString(questionEmbedding);

            List<WikiSectionChunk> similarChunks = chunkRepository.findSimilarChunks(
                    projectExternalId, vectorStr, 5);

            if (similarChunks.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("# Repository Context (Semantic Match)\n\n");
            for (WikiSectionChunk chunk : similarChunks) {
                String heading = chunk.getSectionId();
                if (chunk.getSubsectionId() != null) {
                    heading += " > " + chunk.getSubsectionId();
                }
                context.append("## ").append(heading).append("\n\n");
                context.append(chunk.getContent()).append("\n\n");
            }

            return truncateToTokenLimit(context.toString(), 3000);
        } catch (Exception e) {
            log.warn("Vector search failed for project {}: {}", projectExternalId, e.getMessage());
            return "";
        }
    }

    private float[] embedText(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model("text-embedding-3-small")
                .input(text)
                .build();

        CreateEmbeddingResponse response = openAIClient.embeddings().create(params);

        List<Float> embeddingFloats = response.data().getFirst().embedding();
        float[] embedding = new float[embeddingFloats.size()];
        for (int i = 0; i < embeddingFloats.size(); i++) {
            embedding[i] = embeddingFloats.get(i);
        }
        return embedding;
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncateToTokenLimit(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "...";
    }
}
