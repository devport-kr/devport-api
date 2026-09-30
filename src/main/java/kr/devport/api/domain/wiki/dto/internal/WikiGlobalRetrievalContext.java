package kr.devport.api.domain.wiki.dto.internal;

import java.util.List;

public record WikiGlobalRetrievalContext(
        String groundedContext,
        List<ScoredProject> scoredProjects,
        boolean hasGrounding
) {
    public record ScoredProject(
            String projectExternalId,
            double score,
            List<WikiRetrievedChunk> topChunks
    ) {
    }
}
