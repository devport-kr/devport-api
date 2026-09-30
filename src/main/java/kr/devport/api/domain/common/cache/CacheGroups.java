package kr.devport.api.domain.common.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mapping of cache scopes to related cache groups.
 * Used for scope-based invalidation when webhook signals data changes.
 */
public final class CacheGroups {
    
    private static final Map<CacheScope, Set<String>> SCOPE_TO_CACHES;
    
    static {
        Map<CacheScope, Set<String>> map = new HashMap<>();
        
        // Article scope - detail and summary caches
        map.put(CacheScope.ARTICLE, Set.of(
            CacheNames.ARTICLES,
            CacheNames.TRENDING_TICKER,
            CacheNames.ARTICLE_SEARCH
        ));
        
        // Git repository scope - detail, list, trending, and language-filtered caches
        map.put(CacheScope.GIT_REPO, Set.of(
            CacheNames.GIT_REPOS,
            CacheNames.TRENDING_GIT_REPOS,
            CacheNames.GIT_REPOS_BY_LANGUAGE,
            CacheNames.GITHUB_TRENDING
        ));
        
        // LLM scope - leaderboard, benchmarks, and model caches
        map.put(CacheScope.LLM, Set.of(
            CacheNames.LLM_LEADERBOARD,
            CacheNames.LLM_BENCHMARKS,
            CacheNames.LLM_MODELS
        ));
        
        // Unknown scope - broad invalidation for safety (all critical caches)
        map.put(CacheScope.UNKNOWN, Set.of(
            CacheNames.ARTICLES,
            CacheNames.TRENDING_TICKER,
            CacheNames.ARTICLE_SEARCH,
            CacheNames.GIT_REPOS,
            CacheNames.TRENDING_GIT_REPOS,
            CacheNames.GIT_REPOS_BY_LANGUAGE,
            CacheNames.GITHUB_TRENDING,
            CacheNames.LLM_LEADERBOARD,
            CacheNames.LLM_BENCHMARKS,
            CacheNames.LLM_MODELS
        ));
        
        SCOPE_TO_CACHES = Collections.unmodifiableMap(map);
    }
    
    /**
     * Returns all cache names associated with the given scope.
     * For UNKNOWN scope, returns all critical caches for safety.
     */
    public static Set<String> forScope(CacheScope scope) {
        return SCOPE_TO_CACHES.getOrDefault(scope, Collections.emptySet());
    }
    
    private CacheGroups() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
