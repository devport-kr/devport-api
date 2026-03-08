package kr.devport.api.domain.wiki.dto.internal;

public record WikiRetrievedChunk(
        String sectionId,
        String subsectionId,
        String heading,
        String content,
        double similarityScore,
        double headingScore,
        String sourcePathHint
) {
}
