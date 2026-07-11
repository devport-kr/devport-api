package kr.devport.api.domain.common.webhook

import kr.devport.api.domain.common.cache.CacheInvalidationService
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Handles crawler webhook events and delegates to cache invalidation. Validates webhook authenticity
 * via HMAC signature before processing.
 */
@Service
class CrawlerWebhookService(
    private val cacheInvalidationService: CacheInvalidationService,
    @param:Value("\${app.crawler.webhook-secret:}") private val webhookSecret: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Validates the HMAC signature for a webhook payload. */
    fun validateSignature(
        payload: String,
        signature: String?,
    ): Boolean {
        if (webhookSecret.isBlank()) {
            log.error("CRAWLER_WEBHOOK_SECRET is not configured; rejecting crawler webhook")
            return false
        }

        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            val secretKey = SecretKeySpec(webhookSecret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
            mac.init(secretKey)

            val hmacBytes = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            val computedSignature = HexFormat.of().formatHex(hmacBytes)

            val isValid = computedSignature.equals(signature, ignoreCase = true)
            if (!isValid) {
                log.warn("Rejected crawler webhook because signature validation failed")
            }
            isValid
        } catch (e: GeneralSecurityException) {
            log.error("Failed to compute HMAC signature", e)
            false
        }
    }

    /** Processes a job-completed webhook and triggers cache invalidation. Idempotent. */
    fun handleJobCompleted(request: CrawlerJobCompletedRequest) {
        val scope = request.effectiveScope

        log.info("Processing crawler job completion: jobId={}, scope={}", request.jobId, scope)

        try {
            cacheInvalidationService.invalidateScope(scope, request.jobId ?: "")
            log.info("Successfully invalidated caches for scope={}, jobId={}", scope, request.jobId)
        } catch (e: Exception) {
            // Don't rethrow — the webhook should still return 202 for retry safety.
            log.error("Failed to invalidate caches for scope={}, jobId={}", scope, request.jobId, e)
        }
    }

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
