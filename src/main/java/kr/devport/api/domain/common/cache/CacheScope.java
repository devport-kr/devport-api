package kr.devport.api.domain.common.cache;

/**
 * Domain scopes for webhook-driven cache invalidation.
 * Maps crawler job types to affected cache groups.
 */
public enum CacheScope {
    /**
     * Article-related caches (article list, trending ticker)
     */
    ARTICLE,
    
    /**
     * Git repository caches (repos, trending, by-language)
     */
    GIT_REPO,
    
    /**
     * LLM ranking and benchmark caches
     */
    LLM,
    
    /**
     * Unknown or uncertain scope - triggers broad invalidation for safety
     */
    UNKNOWN
}
