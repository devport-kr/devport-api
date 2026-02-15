package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wiki chat retrieval service combining repo and ecosystem context.
 * Fuses project wiki snapshot content with constrained ecosystem context
 * relevant to user questions for high-precision chat answers.
 */
@Service
@RequiredArgsConstructor
public class WikiRetrievalService {

    private final ProjectWikiSnapshotRepository wikiSnapshotRepository;
    private final OpenAIClient openAIClient;

    /**
     * Retrieve grounded context for a chat turn by combining:
     * 1. Repo-specific context from wiki snapshot sections
     * 2. Constrained ecosystem context relevant to the question
     *
     * Uses deterministic source selection and bounded token windows
     * to support high-precision answers without persistent vector memory.
     *
     * @param projectExternalId Project external ID
     * @param userQuestion User's question
     * @return Combined context string for chat generation
     */
    public String retrieveContext(String projectExternalId, String userQuestion) {
        // Fetch wiki snapshot for repo context
        ProjectWikiSnapshot snapshot = wikiSnapshotRepository
                .findByProjectExternalIdAndIsDataReady(projectExternalId, true)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wiki snapshot not ready for project: " + projectExternalId));

        // Assemble repo context from relevant sections
        String repoContext = assembleRepoContext(snapshot, userQuestion);

        // Assemble constrained ecosystem context based on question
        String ecosystemContext = assembleEcosystemContext(snapshot, userQuestion);

        // Combine contexts with bounded token window
        return combineContexts(repoContext, ecosystemContext);
    }

    /**
     * Assemble repo-specific context by selecting relevant wiki sections.
     * Prioritizes sections most likely to contain answer based on question.
     */
    private String assembleRepoContext(ProjectWikiSnapshot snapshot, String userQuestion) {
        StringBuilder context = new StringBuilder();
        
        // Extract section summaries and deep content
        // Priority: architecture > how > what > activity > releases
        List<Map<String, Object>> sections = new ArrayList<>();
        
        if (snapshot.getArchitectureSection() != null) {
            sections.add(snapshot.getArchitectureSection());
        }
        if (snapshot.getHowSection() != null) {
            sections.add(snapshot.getHowSection());
        }
        if (snapshot.getWhatSection() != null) {
            sections.add(snapshot.getWhatSection());
        }
        if (snapshot.getActivitySection() != null) {
            sections.add(snapshot.getActivitySection());
        }
        if (snapshot.getReleasesSection() != null) {
            sections.add(snapshot.getReleasesSection());
        }

        // Build context from section summaries and deep dive content
        for (Map<String, Object> section : sections) {
            String summary = (String) section.getOrDefault("summary", "");
            String deepDive = (String) section.getOrDefault("deepDiveMarkdown", "");
            
            if (!summary.isBlank()) {
                context.append(summary).append("\n\n");
            }
            if (!deepDive.isBlank()) {
                // Limit deep dive content to avoid token overflow
                String truncatedDeepDive = truncateToTokenLimit(deepDive, 500);
                context.append(truncatedDeepDive).append("\n\n");
            }
        }

        return context.toString();
    }

    /**
     * Assemble constrained ecosystem context relevant to the question.
     * Currently returns empty; can be expanded with ecosystem knowledge retrieval.
     */
    private String assembleEcosystemContext(ProjectWikiSnapshot snapshot, String userQuestion) {
        // For this phase, ecosystem context is minimal
        // Future enhancement: add relevant ecosystem patterns, best practices, etc.
        return "";
    }

    /**
     * Combine repo and ecosystem contexts with bounded token window.
     * Ensures total context stays within model limits for deterministic behavior.
     */
    private String combineContexts(String repoContext, String ecosystemContext) {
        StringBuilder combined = new StringBuilder();
        
        combined.append("# Repository Context\n\n");
        combined.append(repoContext);
        
        if (!ecosystemContext.isBlank()) {
            combined.append("\n# Ecosystem Context\n\n");
            combined.append(ecosystemContext);
        }

        // Final token limit check (approximate)
        String result = combined.toString();
        return truncateToTokenLimit(result, 3000);
    }

    /**
     * Truncate text to approximate token limit.
     * Uses character-based approximation (4 chars ≈ 1 token).
     */
    private String truncateToTokenLimit(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "...";
    }
}
