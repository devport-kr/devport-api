package kr.devport.api.domain.wiki.controller;

import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.service.WikiChatRateLimiter;
import kr.devport.api.domain.wiki.service.WikiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wiki chat controller.
 * Compact payload for right-rail chat module consumption.
 */
@RestController
@RequestMapping("/api/wiki/projects")
@RequiredArgsConstructor
public class WikiChatController {

    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;
    private static final String STREAM_ERROR_MESSAGE = "처리 중 오류가 발생했습니다.";

    private final WikiChatService wikiChatService;
    private final WikiChatRateLimiter rateLimiter;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

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
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rateLimiter.check(userDetails.getId().toString());
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
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rateLimiter.check(userDetails.getId().toString());
        WikiChatResult result = wikiChatService.chatResult(
                request.getSessionId(),
                id,
                request.getQuestion()
        );

        return ResponseEntity.ok(WikiChatResponse.from(result, request.getSessionId()));
    }

    @PostMapping(value = "/{projectExternalId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String projectExternalId,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return streamChatInternal(projectExternalId, request, userDetails);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatByQueryId(
            @RequestParam String id,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return streamChatInternal(id, request, userDetails);
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

    @PreDestroy
    void shutdownStreamExecutor() {
        streamExecutor.close();
    }

    private SseEmitter streamChatInternal(
            String projectExternalId,
            WikiChatRequest request,
            CustomUserDetails userDetails
    ) {
        rateLimiter.check(userDetails.getId().toString());

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emitter.onTimeout(emitter::complete);

        streamExecutor.execute(() -> {
            try {
                WikiChatResult result = wikiChatService.streamChatResult(
                        request.getSessionId(),
                        projectExternalId,
                        request.getQuestion(),
                        token -> sendToken(emitter, token)
                );
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(
                                new StreamDonePayload(
                                        request.getSessionId(),
                                        result.isClarification(),
                                        result.clarificationOptions(),
                                        result.suggestedNextQuestions(),
                                        result.sessionReset()
                                ),
                                MediaType.APPLICATION_JSON
                        ));
                emitter.complete();
            } catch (Exception ex) {
                handleStreamFailure(emitter, ex);
            }
        });

        return emitter;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            if (token != null) {
                // Prepend a space to prevent the SSE client from stripping leading spaces,
                // and replace newlines with "\ndata: " to preserve SSE framing for multi-line tokens.
                String formattedToken = " " + token.replace("\n", "\ndata: ");
                emitter.send(SseEmitter.event().name("token").data(formattedToken, MediaType.TEXT_PLAIN));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send streaming token", e);
        }
    }

    private void handleStreamFailure(SseEmitter emitter, Exception ex) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", STREAM_ERROR_MESSAGE), MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            // Client disconnects are expected here; completeWithError still marks the async request failed.
        }
        emitter.completeWithError(ex);
    }

    private record StreamDonePayload(
            String sessionId,
            boolean isClarification,
            List<String> clarificationOptions,
            List<String> suggestedNextQuestions,
            boolean sessionReset
    ) {
    }
}
