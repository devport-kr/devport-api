package kr.devport.api.domain.wiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.devport.api.domain.common.logging.LoggingContext;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.exception.WikiChatRateLimitExceededException;
import kr.devport.api.domain.wiki.service.WikiChatApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Supports both authenticated and anonymous users (anon: 1 req/day via IP limit).
 */
@Slf4j
@RestController
@RequestMapping("/api/wiki/projects")
@RequiredArgsConstructor
public class WikiChatController {

    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WikiChatApplicationService chatApplicationService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PostMapping("/{projectExternalId}/chat")
    public ResponseEntity<WikiChatResponse> chat(
            @PathVariable String projectExternalId,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(chatApplicationService.chatProject(
                projectExternalId,
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest)
        ));
    }

    @PostMapping("/chat")
    public ResponseEntity<WikiChatResponse> chatByQueryId(
            @RequestParam String id,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(chatApplicationService.chatProject(
                id,
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest)
        ));
    }

    @PostMapping(value = "/{projectExternalId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String projectExternalId,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        return streamChatInternal(projectExternalId, request, userDetails, httpRequest);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatByQueryId(
            @RequestParam String id,
            @Valid @RequestBody WikiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        return streamChatInternal(id, request, userDetails, httpRequest);
    }

    @DeleteMapping("/{projectExternalId}/chat/sessions/{sessionId}")
    public ResponseEntity<Void> clearSession(
            @PathVariable String projectExternalId,
            @PathVariable String sessionId
    ) {
        chatApplicationService.clearProjectSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PreDestroy
    void shutdownStreamExecutor() {
        streamExecutor.close();
    }

    private SseEmitter streamChatInternal(
            String projectExternalId,
            WikiChatRequest request,
            CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        // HttpServletRequest is not safe to use inside async threads — extract synchronously
        final Long userId = extractUserId(userDetails);
        final String clientIp = extractClientIp(userDetails, httpRequest);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emitter.onTimeout(emitter::complete);

        streamExecutor.execute(LoggingContext.wrap(() -> {
            try {
                WikiChatResult result = chatApplicationService.streamProject(
                        projectExternalId,
                        request,
                        userId,
                        clientIp,
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
        }));

        return emitter;
    }

    private Long extractUserId(CustomUserDetails userDetails) {
        return userDetails != null ? userDetails.getId() : null;
    }

    private String extractClientIp(CustomUserDetails userDetails, HttpServletRequest request) {
        return userDetails == null ? extractIp(request) : null;
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            if (token != null) {
                String jsonToken = OBJECT_MAPPER.writeValueAsString(token);
                emitter.send(SseEmitter.event().name("token").data(jsonToken, MediaType.APPLICATION_JSON));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send streaming token", e);
        }
    }

    private record StreamDonePayload(
            String sessionId,
            boolean isClarification,
            List<String> clarificationOptions,
            List<String> suggestedNextQuestions,
            boolean sessionReset
    ) {
    }

    private void handleStreamFailure(SseEmitter emitter, Exception ex) {
        log.error("Wiki chat stream failed", ex);
        try {
            String message = ex instanceof WikiChatRateLimitExceededException
                    ? ex.getMessage()
                    : "서비스 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message), MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }
}
