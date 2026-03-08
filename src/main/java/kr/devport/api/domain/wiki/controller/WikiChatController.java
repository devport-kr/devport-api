package kr.devport.api.domain.wiki.controller;

import jakarta.validation.Valid;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.service.WikiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Wiki chat controller.
 * Compact payload for right-rail chat module consumption.
 */
@RestController
@RequestMapping("/api/wiki/projects")
@RequiredArgsConstructor
public class WikiChatController {

    private final WikiChatService wikiChatService;

    /**
     * POST /api/wiki/projects/{projectExternalId}/chat
     *
     * Chat endpoint with precise answers, clarifying prompts on uncertainty,
     * and session-scoped continuity.
     *
     * @param projectExternalId Project external ID
     * @param request Chat request with question and session ID
     * @return Chat response with answer and session metadata
     */
    @PostMapping("/{projectExternalId}/chat")
    public ResponseEntity<WikiChatResponse> chat(
            @PathVariable String projectExternalId,
            @Valid @RequestBody WikiChatRequest request
    ) {
        WikiChatResult result = wikiChatService.chatResult(
                request.getSessionId(),
                projectExternalId,
                request.getQuestion()
        );

        return ResponseEntity.ok(WikiChatResponse.from(result, request.getSessionId()));
    }

    /**
     * POST /api/wiki/projects/chat
     *
     * Chat endpoint using query param for project ID to avoid encoded slash issues.
     *
     * @param id Project external ID via query parameter
     * @param request Chat request with question and session ID
     * @return Chat response with answer and session metadata
     */
    @PostMapping("/chat")
    public ResponseEntity<WikiChatResponse> chatByQueryId(
            @RequestParam String id,
            @Valid @RequestBody WikiChatRequest request
    ) {
        WikiChatResult result = wikiChatService.chatResult(
                request.getSessionId(),
                id,
                request.getQuestion()
        );

        return ResponseEntity.ok(WikiChatResponse.from(result, request.getSessionId()));
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
    @DeleteMapping("/{projectExternalId}/chat/sessions/{sessionId}")
    public ResponseEntity<Void> clearSession(
            @PathVariable String projectExternalId,
            @PathVariable String sessionId
    ) {
        wikiChatService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

}
