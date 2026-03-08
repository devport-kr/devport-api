package kr.devport.api.domain.wiki.dto.internal;

import java.util.List;

public record WikiChatResult(
        String answer,
        boolean isClarification,
        List<String> clarificationOptions,
        List<String> suggestedNextQuestions,
        boolean usedPreviousContext,
        boolean sessionReset
) {
}
