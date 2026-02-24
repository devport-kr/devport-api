package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wiki chat retrieval service combining repo and ecosystem context.
 * Uses vector similarity search when chunks are available, falling back
 * to deterministic section-priority selection for backward compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiRetrievalService {

    private final ProjectWikiSnapshotRepository wikiSnapshotRepository;
    private final WikiSectionChunkRepository chunkRepository;
    private final OpenAIClient openAIClient;

    /**
     * Retrieve grounded context for a chat turn.
     * Attempts vector similarity search first; falls back to section-priority
     * selection if no chunks exist for this project.
     */
    public String retrieveContext(String projectExternalId, String userQuestion) {
        ProjectWikiSnapshot snapshot = wikiSnapshotRepository
                .findByProjectExternalIdAndIsDataReady(projectExternalId, true)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wiki snapshot not ready for project: " + projectExternalId));

        // Try vector similarity search first
        String vectorContext = retrieveVectorContext(projectExternalId, userQuestion);
        if (vectorContext != null) {
            return vectorContext;
        }

        // Fallback to deterministic section-priority selection
        log.debug("No vector chunks for project {}, using section-priority fallback", projectExternalId);
        String repoContext = assembleRepoContext(snapshot, userQuestion);
        String ecosystemContext = assembleEcosystemContext(snapshot, userQuestion);
        return combineContexts(repoContext, ecosystemContext);
    }

    /**
     * Retrieve context via vector similarity search.
     * Returns null if no chunks exist for this project (triggers fallback).
     */
    private String retrieveVectorContext(String projectExternalId, String userQuestion) {
        try {
            List<WikiSectionChunk> existingChunks = chunkRepository.findByProjectExternalId(projectExternalId);
            if (existingChunks.isEmpty()) {
                return null;
            }

            // Embed the user question
            float[] questionEmbedding = embedText(userQuestion);
            String vectorStr = toVectorString(questionEmbedding);

            // Find top 5 most similar chunks
            List<WikiSectionChunk> similarChunks = chunkRepository.findSimilarChunks(
                    projectExternalId, vectorStr, 5);

            if (similarChunks.isEmpty()) {
                return null;
            }

            // Assemble context from matched chunks
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
            log.warn("Vector search failed for project {}, falling back to priority", projectExternalId, e);
            return null;
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

    // ── Fallback: section-priority selection ──

    private String assembleRepoContext(ProjectWikiSnapshot snapshot, String userQuestion) {
        StringBuilder context = new StringBuilder();
        List<Map<String, Object>> sections = prioritizeSections(snapshot.getResolvedSections());
        Set<String> hidden = Set.copyOf(snapshot.getHiddenSectionsOrEmpty());

        for (Map<String, Object> section : sections) {
            String sectionId = String.valueOf(section.getOrDefault("sectionId", ""));
            if (hidden.contains(sectionId)) {
                continue;
            }

            String summary = (String) section.getOrDefault("summary", "");
            String deepDive = (String) section.getOrDefault("deepDiveMarkdown", section.getOrDefault("deepDive", ""));

            if (!summary.isBlank()) {
                context.append(summary).append("\n\n");
            }
            if (!deepDive.isBlank()) {
                String truncatedDeepDive = truncateToTokenLimit(deepDive, 500);
                context.append(truncatedDeepDive).append("\n\n");
            }
        }

        return context.toString();
    }

    private List<Map<String, Object>> prioritizeSections(List<Map<String, Object>> sections) {
        List<Map<String, Object>> prioritized = new ArrayList<>();
        addBySectionId(prioritized, sections, "architecture");
        addBySectionId(prioritized, sections, "how");
        addBySectionId(prioritized, sections, "what");
        addBySectionId(prioritized, sections, "activity");
        addBySectionId(prioritized, sections, "releases");

        for (Map<String, Object> section : sections) {
            if (!prioritized.contains(section)) {
                prioritized.add(section);
            }
        }
        return prioritized;
    }

    private void addBySectionId(List<Map<String, Object>> target, List<Map<String, Object>> source, String sectionId) {
        for (Map<String, Object> section : source) {
            if (sectionId.equals(String.valueOf(section.getOrDefault("sectionId", "")))) {
                target.add(section);
            }
        }
    }

    private String assembleEcosystemContext(ProjectWikiSnapshot snapshot, String userQuestion) {
        return "";
    }

    private String combineContexts(String repoContext, String ecosystemContext) {
        StringBuilder combined = new StringBuilder();

        combined.append("# Repository Context\n\n");
        combined.append(repoContext);

        if (!ecosystemContext.isBlank()) {
            combined.append("\n# Ecosystem Context\n\n");
            combined.append(ecosystemContext);
        }

        String result = combined.toString();
        return truncateToTokenLimit(result, 3000);
    }

    private String truncateToTokenLimit(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "...";
    }
}
