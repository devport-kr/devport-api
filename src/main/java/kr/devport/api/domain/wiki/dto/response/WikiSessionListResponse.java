package kr.devport.api.domain.wiki.dto.response;

import java.util.List;

public record WikiSessionListResponse(
        List<WikiSessionResponse> sessions,
        int totalPages,
        long totalElements,
        int page,
        int size
) {
}
