package kr.devport.api.domain.common.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Explicit TTL ownership for all critical caches.
 * Single source of truth for cache expiration policies.
 */
public final class CacheTtlPolicy {
    
    private static final Map<String, Duration> TTL_MAP;
    
    static {
        Map<String, Duration> map = new HashMap<>();
        
        // Article caches - moderate TTL for frequently updated content
        map.put(CacheNames.ARTICLES, Duration.ofMinutes(10));
        map.put(CacheNames.TRENDING_TICKER, Duration.ofMinutes(5));
        
        // Git repository caches - varied TTL based on update frequency
        map.put(CacheNames.GIT_REPOS, Duration.ofMinutes(10));
        map.put(CacheNames.TRENDING_GIT_REPOS, Duration.ofHours(1));
        map.put(CacheNames.GIT_REPOS_BY_LANGUAGE, Duration.ofMinutes(30));
        map.put(CacheNames.GITHUB_TRENDING, Duration.ofHours(1));
        
        // LLM caches - longer TTL for relatively stable data
        map.put(CacheNames.LLM_LEADERBOARD, Duration.ofHours(24));
        map.put(CacheNames.LLM_BENCHMARKS, Duration.ofHours(24));
        map.put(CacheNames.LLM_MODELS, Duration.ofHours(24));

        // Wiki caches - content changes infrequently (ingested externally)
        map.put(CacheNames.WIKI_PROJECTS, Duration.ofMinutes(30));
        map.put(CacheNames.WIKI_PROJECT_PAGE, Duration.ofMinutes(30));
        
        TTL_MAP = Collections.unmodifiableMap(map);
    }
    
    /**
     * Default TTL for caches not explicitly defined
     */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    
    /**
     * Returns the TTL duration for the given cache name.
     * If not explicitly defined, returns DEFAULT_TTL.
     */
    public static Duration getTtl(String cacheName) {
        return TTL_MAP.getOrDefault(cacheName, DEFAULT_TTL);
    }
    
    /**
     * Returns all cache configurations as a map.
     * Used for initializing the cache manager.
     */
    public static Map<String, Duration> getAllTtls() {
        return TTL_MAP;
    }
    
    private CacheTtlPolicy() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
