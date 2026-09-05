package kr.devport.api.domain.common.webhook

import jakarta.validation.Valid
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Webhook endpoint for crawler-initiated cache invalidation. Receives job-completed events from the
 * external crawler system.
 */
@RestController
@RequestMapping("/api/webhooks/crawler")
class CrawlerWebhookController(
    private val crawlerWebhookService: CrawlerWebhookService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Receives a crawler job-completed webhook. Validates the HMAC signature before processing and
     * returns 202 Accepted for idempotent retry safety (401 on invalid signature).
     */
    @PostMapping("/job-completed")
    fun handleJobCompleted(
        @Valid @RequestBody request: CrawlerJobCompletedRequest,
        @RequestBody rawPayload: String,
    ): ResponseEntity<Map<String, Any?>> {
        log.debug("Received crawler webhook: jobId={}", request.jobId)

        if (!crawlerWebhookService.validateSignature(rawPayload, request.signature)) {
            log.warn("Rejected webhook with invalid signature: jobId={}", request.jobId)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "error" to "Invalid signature",
                    "job_id" to request.jobId,
                ),
            )
        }

        crawlerWebhookService.handleJobCompleted(request)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "status" to "accepted",
                "job_id" to request.jobId,
                "scope" to request.effectiveScope,
            ),
        )
    }
}
