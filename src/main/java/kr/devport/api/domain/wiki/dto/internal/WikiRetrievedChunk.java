package kr.devport.api.domain.wiki.dto.internal;

public record WikiRetrievedChunk(
        String sectionId,
        String subsectionId,
        String chunkType,
        String heading,
        String content,
        double similarityScore,
        double headingScore,
        Double rerankScore,
        String sourcePathHint
) {
}
