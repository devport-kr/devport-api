package kr.devport.api.domain.wiki.controller;

import jakarta.validation.Valid;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.service.WikiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki chat controller.
 * Default responses are citation-light; citations returned only when requested.
 * Compact payload for right-rail chat module consumption.
 */
@RestController
@RequestMapping("/api/wiki/projects/{projectExternalId}/chat")
@RequiredArgsConstructor
public class WikiChatController {

    private final WikiChatService wikiChatService;

    /**
     * POST /api/wiki/projects/{projectExternalId}/chat
     *
     * Chat endpoint with precise answers, clarifying prompts on uncertainty,
     * and optional citations on demand.
     *
     * @param projectExternalId Project external ID
     * @param request Chat request with question and session ID
     * @return Chat response with answer and optional citations
     */
    @PostMapping
    public ResponseEntity<WikiChatResponse> chat(
            @PathVariable String projectExternalId,
            @Valid @RequestBody WikiChatRequest request
    ) {
        // Generate chat answer with grounded context
        String answer = wikiChatService.chat(
                request.getSessionId(),
                projectExternalId,
                request.getQuestion()
        );

        // Detect if answer is a clarification question
        boolean isClarification = answer.contains("?");

        // Build response without citations by default
        WikiChatResponse.WikiChatResponseBuilder responseBuilder = WikiChatResponse.builder()
                .answer(answer)
                .isClarification(isClarification)
                .sessionId(request.getSessionId());

        // Include citations only when requested
        if (request.isIncludeCitations()) {
            responseBuilder.citations(extractCitations(answer));
        }

        return ResponseEntity.ok(responseBuilder.build());
    }

    /**
     * DELETE /api/wiki/projects/{projectExternalId}/chat/sessions/{sessionId}
     *
     * Clear session memory explicitly.
     *
     * @param projectExternalId Project external ID
     * @param sessionId Session identifier
     * @return No content response
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> clearSession(
            @PathVariable String projectExternalId,
            @PathVariable String sessionId
    ) {
        wikiChatService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract citations from answer content (placeholder for future implementation).
     * In this phase, citations are minimal.
     */
    private List<WikiChatResponse.Citation> extractCitations(String answer) {
        // Future enhancement: parse structured citations from answer
        // For now, return empty list
        return List.of();
    }
}
