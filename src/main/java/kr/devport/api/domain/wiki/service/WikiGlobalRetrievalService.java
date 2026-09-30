package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext;
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext.ScoredProject;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievedChunk;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepositoryCustom.ScoredChunkRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-project RAG retrieval for global wiki chat.
 * Finds the most relevant projects across all wiki content for a given question.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiGlobalRetrievalService {

    private static final int GLOBAL_CANDIDATE_LIMIT = 20;
    private static final int MAX_PROJECTS = 5;
    private static final int CHUNKS_PER_PROJECT = 2;
    private static final int MAX_CONTEXT_TOKENS = 4000;

    private final WikiSectionChunkRepository chunkRepository;
    private final OpenAIClient openAIClient;

    public WikiGlobalRetrievalContext retrieve(String question) {
        try {
            String vectorStr = toVectorString(embedText(question));
            List<ScoredChunkRow> topChunks = chunkRepository.findSimilarChunksGlobalWithScore(vectorStr, GLOBAL_CANDIDATE_LIMIT);

            if (topChunks.isEmpty()) {
                return new WikiGlobalRetrievalContext("", List.of(), false);
            }

            Map<String, List<IndexedChunk>> byProject = new LinkedHashMap<>();
            for (ScoredChunkRow row : topChunks) {
                byProject.computeIfAbsent(row.chunk().getProjectExternalId(), ignored -> new ArrayList<>())
                        .add(new IndexedChunk(row.chunk(), row.score()));
            }

            List<ScoredProject> scoredProjects = byProject.entrySet().stream()
                    .map(entry -> {
                        List<IndexedChunk> chunks = entry.getValue();
                        double projectScore = chunks.stream()
                                .sorted(Comparator.comparingDouble(IndexedChunk::similarity).reversed())
                                .limit(2)
                                .mapToDouble(IndexedChunk::similarity)
                                .sum();
                        List<WikiRetrievedChunk> bestChunks = chunks.stream()
                                .sorted(Comparator.comparingDouble(IndexedChunk::similarity).reversed())
                                .limit(CHUNKS_PER_PROJECT)
                                .map(ic -> new WikiRetrievedChunk(
                                        ic.chunk().getSectionId(),
                                        ic.chunk().getSubsectionId(),
                                        ic.chunk().getChunkType(),
                                        resolveHeading(ic.chunk()),
                                        ic.chunk().getContent(),
                                        ic.similarity(),
                                        0.0d,
                                        null,
                                        null
                                ))
                                .toList();
                        return new ScoredProject(entry.getKey(), projectScore, bestChunks);
                    })
                    .sorted(Comparator.comparingDouble(ScoredProject::score).reversed())
                    .limit(MAX_PROJECTS)
                    .toList();

            return new WikiGlobalRetrievalContext(buildContext(scoredProjects), scoredProjects, !scoredProjects.isEmpty());
        } catch (Exception e) {
            log.warn("wiki-global-retrieval: retrieval failed: {}", e.getMessage());
            return new WikiGlobalRetrievalContext("", List.of(), false);
        }
    }

    private String buildContext(List<ScoredProject> scoredProjects) {
        StringBuilder sb = new StringBuilder("# Multi-Project Context\n\n");
        for (ScoredProject project : scoredProjects) {
            sb.append("## Project: ").append(project.projectExternalId()).append("\n\n");
            for (WikiRetrievedChunk chunk : project.topChunks()) {
                sb.append("### ").append(chunk.heading()).append("\n\n");
                sb.append(chunk.content()).append("\n\n");
            }
        }
        return truncateToTokenLimit(sb.toString(), MAX_CONTEXT_TOKENS);
    }

    private String resolveHeading(WikiSectionChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata();
        if (metadata != null) {
            Object titleKo = metadata.get("titleKo");
            if (titleKo != null && !String.valueOf(titleKo).isBlank()) {
                return String.valueOf(titleKo);
            }
        }
        if (chunk.getSubsectionId() != null && !chunk.getSubsectionId().isBlank()) {
            return chunk.getSectionId() + " > " + chunk.getSubsectionId();
        }
        return chunk.getSectionId();
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
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncateToTokenLimit(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }

    private record IndexedChunk(WikiSectionChunk chunk, double similarity) {
    }
}
