package kr.devport.api.domain.wiki.dto.response;

import java.time.LocalDateTime;

public record WikiSessionResponse(
        String sessionId,
        String title,
        String sessionType,
        String projectExternalId,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt,
        int messageCount
) {
}
