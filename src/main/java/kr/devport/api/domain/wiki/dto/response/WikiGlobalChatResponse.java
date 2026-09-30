package kr.devport.api.domain.wiki.dto.response;

import java.util.List;

public record WikiGlobalChatResponse(
        String answer,
        List<RelatedProjectResponse> relatedProjects,
        boolean hasRelatedProjects,
        String sessionId
) {
}
