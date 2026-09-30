package kr.devport.api.domain.wiki.dto.response;

import java.time.LocalDateTime;

public record WikiMessageResponse(
        String role,
        String content,
        boolean isClarification,
        LocalDateTime createdAt
) {
}
