package kr.devport.api.domain.wiki.dto.internal;

import java.util.List;

public record WikiGlobalChatResult(
        String answer,
        List<RelatedProjectLlmOutput> relatedProjects,
        boolean hasRelatedProjects,
        boolean sessionReset
) {
    public record RelatedProjectLlmOutput(
            String projectExternalId,
            String relevanceReason
    ) {
    }
}
