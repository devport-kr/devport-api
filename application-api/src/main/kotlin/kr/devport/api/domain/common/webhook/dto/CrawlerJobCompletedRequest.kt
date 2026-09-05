package kr.devport.api.domain.common.webhook.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import kr.devport.api.domain.common.cache.CacheScope

/**
 * Webhook payload for crawler job-completion events. Received from the external crawler to signal
 * data changes that require cache invalidation.
 */
data class CrawlerJobCompletedRequest(
    @param:JsonProperty("job_id")
    @field:NotBlank(message = "Job ID is required")
    val jobId: String? = null,
    @param:JsonProperty("scope")
    val scope: CacheScope? = null,
    @param:JsonProperty("completed_at")
    val completedAt: String? = null,
    @param:JsonProperty("freshness_signals")
    val freshnessSignals: List<Map<String, Any>>? = null,
    @param:JsonProperty("signature")
    @field:NotBlank(message = "Signature is required")
    val signature: String? = null,
) {
    /** Effective scope, defaulting to UNKNOWN when null for safety. */
    val effectiveScope: CacheScope
        get() = scope ?: CacheScope.UNKNOWN

    fun getFreshnessSignalsOrEmpty(): List<Map<String, Any>> = freshnessSignals ?: emptyList()
}
