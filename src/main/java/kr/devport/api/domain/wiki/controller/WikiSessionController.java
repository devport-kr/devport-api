package kr.devport.api.domain.wiki.controller;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.wiki.dto.response.WikiMessageResponse;
import kr.devport.api.domain.wiki.dto.response.WikiSessionListResponse;
import kr.devport.api.domain.wiki.service.WikiChatSessionPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki chat session history endpoints. All require authentication.
 */
@RestController
@RequestMapping("/api/wiki/sessions")
@RequiredArgsConstructor
public class WikiSessionController {

    private final WikiChatSessionPersistenceService persistenceService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<WikiSessionListResponse> getAllSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = resolveUser(userDetails);
        return ResponseEntity.ok(persistenceService.getUserSessions(user, page, size));
    }

    @GetMapping("/project")
    public ResponseEntity<WikiSessionListResponse> getProjectSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String externalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = resolveUser(userDetails);
        return ResponseEntity.ok(persistenceService.getProjectSessions(user, externalId, page, size));
    }

    @GetMapping("/global")
    public ResponseEntity<WikiSessionListResponse> getGlobalSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = resolveUser(userDetails);
        return ResponseEntity.ok(persistenceService.getGlobalSessions(user, page, size));
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<WikiMessageResponse>> getSessionMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId
    ) {
        List<WikiMessageResponse> messages = persistenceService.loadSessionMessages(sessionId, userDetails.getId());
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId
    ) {
        persistenceService.deleteSession(sessionId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(CustomUserDetails userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
