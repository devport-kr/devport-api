package kr.devport.api.domain.wiki.dto.internal;

import java.util.List;

public record WikiRetrievalContext(
        String projectExternalId,
        String groundedContext,
        boolean hasGrounding,
        boolean weakGrounding,
        List<WikiRetrievedChunk> chunks,
        List<String> suggestedNextQuestions
) {
}
