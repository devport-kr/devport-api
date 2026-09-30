package kr.devport.api.domain.common.webhook;

import kr.devport.api.domain.common.cache.CacheInvalidationService;
import kr.devport.api.domain.common.cache.CacheScope;
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Handles crawler webhook events and delegates to cache invalidation.
 * Validates webhook authenticity via HMAC signature before processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerWebhookService {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private final CacheInvalidationService cacheInvalidationService;
    
    @Value("${app.crawler.webhook-secret:}")
    private String webhookSecret;
    
    /**
     * Validates HMAC signature for webhook payload.
     * 
     * @param payload JSON payload received
     * @param signature HMAC signature from webhook
     * @return true if signature is valid
     */
    public boolean validateSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("CRAWLER_WEBHOOK_SECRET is not configured; rejecting crawler webhook");
            return false;
        }
        
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(secretKey);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hmacBytes);
            
            boolean isValid = computedSignature.equalsIgnoreCase(signature);

            if (!isValid) {
                log.warn("Rejected crawler webhook because signature validation failed");
            }
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC signature", e);
            return false;
        }
    }
    
    /**
     * Processes job-completed webhook and triggers cache invalidation.
     * Idempotent - repeated calls for same job_id are safe.
     * 
     * @param request Validated webhook payload
     */
    public void handleJobCompleted(CrawlerJobCompletedRequest request) {
        CacheScope scope = request.getEffectiveScope();
        
        log.info("Processing crawler job completion: jobId={}, scope={}", 
            request.getJobId(), scope);
        
        try {
            cacheInvalidationService.invalidateScope(scope, request.getJobId());
            
            log.info("Successfully invalidated caches for scope={}, jobId={}", 
                scope, request.getJobId());
                
        } catch (Exception e) {
            log.error("Failed to invalidate caches for scope={}, jobId={}", 
                scope, request.getJobId(), e);
            // Don't throw - webhook should still return 202 for retry safety
        }
    }
}
