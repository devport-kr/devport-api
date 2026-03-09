package kr.devport.api.domain.wiki.dto.response;

public record RelatedProjectResponse(
        String projectExternalId,
        String fullName,
        String description,
        String relevanceReason,
        int stars
) {
}
