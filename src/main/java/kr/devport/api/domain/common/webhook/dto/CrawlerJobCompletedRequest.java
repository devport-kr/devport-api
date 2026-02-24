package kr.devport.api.domain.common.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import kr.devport.api.domain.common.cache.CacheScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Webhook payload for crawler job completion events.
 * Received from external crawler system to signal data changes requiring cache invalidation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerJobCompletedRequest {
    
    /**
     * Job identifier for idempotency and logging.
     */
    @NotBlank(message = "Job ID is required")
    @JsonProperty("job_id")
    private String jobId;
    
    /**
     * Scope of the data change (ARTICLE, GIT_REPO, LLM, or UNKNOWN).
     * Null/missing scope is treated as UNKNOWN for safety.
     */
    @JsonProperty("scope")
    private CacheScope scope;
    
    /**
     * Optional timestamp of job completion (ISO-8601).
     */
    @JsonProperty("completed_at")
    private String completedAt;

    /**
     * Freshness-only payloads emitted by crawler wiki stage.
     * Narrative wiki content must not be provided by crawler.
     */
    @JsonProperty("freshness_signals")
    private List<Map<String, Object>> freshnessSignals;
    
    /**
     * HMAC signature for authentication.
     * Computed as HMAC-SHA256(payload_json, CRAWLER_WEBHOOK_SECRET).
     */
    @JsonProperty("signature")
    @NotBlank(message = "Signature is required")
    private String signature;
    
    /**
     * Returns effective scope, defaulting to UNKNOWN when null for safety.
     */
    public CacheScope getEffectiveScope() {
        return scope != null ? scope : CacheScope.UNKNOWN;
    }

    public List<Map<String, Object>> getFreshnessSignalsOrEmpty() {
        return freshnessSignals == null ? List.of() : freshnessSignals;
    }
}
