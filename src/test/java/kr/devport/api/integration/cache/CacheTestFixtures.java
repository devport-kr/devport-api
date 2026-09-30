package kr.devport.api.integration.cache;

import kr.devport.api.domain.common.cache.CacheScope;
import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reusable fixtures and utilities for cache integration tests.
 * Provides deterministic setup/teardown and helper methods for webhook/cache interactions.
 */
public final class CacheTestFixtures {
    
    private static final String WEBHOOK_SECRET = "test-webhook-secret-key-for-integration-tests";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    /**
     * Clears all caches to ensure test isolation.
     */
    public static void clearAllCaches(CacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
    
    /**
     * Clears Redis database completely (for integration test isolation).
     */
    public static void clearRedis(RedisTemplate<String, Object> redisTemplate) {
        redisTemplate.getConnectionFactory()
            .getConnection()
            .serverCommands()
            .flushDb();
    }
    
    /**
     * Verifies cache is empty for given cache name.
     */
    public static boolean isCacheEmpty(CacheManager cacheManager, String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return true;
        }
        return cache.get(key) == null;
    }
    
    /**
     * Verifies cache contains entry for given cache name and key.
     */
    public static boolean isCachePopulated(CacheManager cacheManager, String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return false;
        }
        return cache.get(key) != null;
    }
    
    /**
     * Creates webhook request with valid HMAC signature.
     */
    public static CrawlerJobCompletedRequest createWebhookRequest(String jobId, CacheScope scope) {
        // Build payload first for signature generation
        String completedAt = String.valueOf(System.currentTimeMillis());
        String payload = buildPayload(jobId, scope, completedAt);
        String signature = generateHmacSignature(payload);
        
        return CrawlerJobCompletedRequest.builder()
            .jobId(jobId)
            .scope(scope)
            .completedAt(completedAt)
            .signature(signature)
            .build();
    }
    

    
    /**
     * Waits for cache TTL to expire (with small buffer).
     */
    public static void waitForCacheTtl(long ttlMs) throws InterruptedException {
        Thread.sleep(ttlMs + 100); // TTL + 100ms buffer
    }
    
    /**
     * Waits for async cache operations to complete.
     */
    public static void waitForCacheSync() throws InterruptedException {
        Thread.sleep(50); // Small delay for cache writes
    }
    
    private CacheTestFixtures() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Builds JSON payload for webhook signature verification.
     * Must match exact format expected by webhook service.
     */
    private static String buildPayload(String jobId, CacheScope scope, String completedAt) {
        String scopeValue = scope != null ? scope.name() : "null";
        return String.format("{\"job_id\":\"%s\",\"scope\":\"%s\",\"completed_at\":\"%s\"}",
            jobId,
            scopeValue,
            completedAt);
    }
    
    /**
     * Generates HMAC-SHA256 signature for webhook authentication.
     */
    private static String generateHmacSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(secretKey);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
