package kr.devport.api.domain.common.cache

/**
 * Domain scopes for webhook-driven cache invalidation. Maps crawler job types to affected cache groups.
 * UNKNOWN triggers broad invalidation for safety.
 */
enum class CacheScope {
    ARTICLE,
    GIT_REPO,
    LLM,
    UNKNOWN,
}
