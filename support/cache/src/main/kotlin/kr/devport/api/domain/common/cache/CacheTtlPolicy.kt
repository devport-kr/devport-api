package kr.devport.api.domain.common.cache

import java.time.Duration

/** Explicit TTL ownership for all critical caches — the single source of truth for expiration. */
object CacheTtlPolicy {
    /** Default TTL for caches not explicitly defined. */
    val DEFAULT_TTL: Duration = Duration.ofMinutes(5)

    private val TTL_MAP: Map<String, Duration> =
        mapOf(
            CacheNames.ARTICLES to Duration.ofMinutes(10),
            CacheNames.TRENDING_TICKER to Duration.ofMinutes(5),
            CacheNames.GIT_REPOS to Duration.ofMinutes(10),
            CacheNames.TRENDING_GIT_REPOS to Duration.ofHours(1),
            CacheNames.GIT_REPOS_BY_LANGUAGE to Duration.ofMinutes(30),
            CacheNames.GITHUB_TRENDING to Duration.ofHours(1),
            CacheNames.LLM_LEADERBOARD to Duration.ofHours(24),
            CacheNames.LLM_BENCHMARKS to Duration.ofHours(24),
            CacheNames.LLM_MODELS to Duration.ofHours(24),
            CacheNames.WIKI_PROJECTS to Duration.ofMinutes(30),
            CacheNames.WIKI_PROJECT_PAGE to Duration.ofMinutes(30),
        )

    /** TTL for the given cache name, or [DEFAULT_TTL] if not explicitly defined. */
    fun getTtl(cacheName: String): Duration = TTL_MAP[cacheName] ?: DEFAULT_TTL

    /** All cache TTLs; used to initialize the cache manager. */
    fun getAllTtls(): Map<String, Duration> = TTL_MAP
}
