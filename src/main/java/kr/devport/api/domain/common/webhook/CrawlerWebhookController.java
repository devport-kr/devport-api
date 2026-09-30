package kr.devport.api.domain.common.webhook;

import jakarta.validation.Valid;
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook endpoint for crawler-initiated cache invalidation.
 * Receives job-completed events from external crawler system.
 */
@RestController
@RequestMapping("/api/webhooks/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerWebhookController {
    
    private final CrawlerWebhookService crawlerWebhookService;
    
    /**
     * Receives crawler job-completed webhook.
     * 
     * Validates HMAC signature before processing.
     * Returns 202 Accepted for idempotent retry safety.
     * 
     * @param request Job completion payload with signature
     * @param rawPayload Raw JSON for signature verification
     * @return 202 on success, 401 on invalid signature, 400 on validation error
     */
    @PostMapping("/job-completed")
    public ResponseEntity<Map<String, Object>> handleJobCompleted(
            @Valid @RequestBody CrawlerJobCompletedRequest request,
            @RequestBody String rawPayload) {
        
        log.debug("Received crawler webhook: jobId={}", request.getJobId());
        
        // Validate HMAC signature
        if (!crawlerWebhookService.validateSignature(rawPayload, request.getSignature())) {
            log.warn("Rejected webhook with invalid signature: jobId={}", request.getJobId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Invalid signature",
                    "job_id", request.getJobId()
                ));
        }
        
        // Process webhook (idempotent)
        crawlerWebhookService.handleJobCompleted(request);
        
        // Return 202 Accepted for retry-safe semantics
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "status", "accepted",
                "job_id", request.getJobId(),
                "scope", request.getEffectiveScope()
            ));
    }
}
