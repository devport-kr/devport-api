package kr.devport.api.domain.wiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.devport.api.domain.common.logging.LoggingContext;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.request.WikiGlobalChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global wiki chat controller — discovers relevant projects across all wikis.
 * Supports both authenticated and anonymous users (anon: 1 req/day via IP limit).
 */
@RestController
@RequestMapping("/api/wiki/chat")
@RequiredArgsConstructor
@Slf4j
public class WikiGlobalChatController {

    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;
    private static final String STREAM_ERROR_MESSAGE = "처리 중 오류가 발생했습니다.";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WikiChatApplicationService chatApplicationService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PostMapping
    public ResponseEntity<WikiGlobalChatResponse> chat(
            @Valid @RequestBody WikiGlobalChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(chatApplicationService.chatGlobal(
                request,
                extractUserId(userDetails),
                extractClientIp(userDetails, httpRequest)
        ));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Valid @RequestBody WikiGlobalChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        final Long userId = extractUserId(userDetails);
        final String clientIp = extractClientIp(userDetails, httpRequest);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        emitter.onTimeout(emitter::complete);

        streamExecutor.execute(LoggingContext.wrap(() -> {
            try {
                WikiGlobalChatResponse response = chatApplicationService.streamGlobal(
                        request,
                        userId,
                        clientIp,
                        token -> sendToken(emitter, token)
                );
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(response, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception ex) {
                handleStreamFailure(emitter, ex);
            }
        }));

        return emitter;
    }

    @PreDestroy
    void shutdownStreamExecutor() {
        streamExecutor.close();
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

    private void handleStreamFailure(SseEmitter emitter, Exception ex) {
        log.error("Wiki global chat stream failed", ex);
        try {
            String message = ex instanceof WikiChatRateLimitExceededException
                    ? ex.getMessage()
                    : STREAM_ERROR_MESSAGE;
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message), MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }
}
