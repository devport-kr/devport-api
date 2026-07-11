package kr.devport.api.domain.common.cache

/**
 * Mapping of cache scopes to related cache groups. Used for scope-based invalidation when a webhook
 * signals a data change.
 */
object CacheGroups {
    private val SCOPE_TO_CACHES: Map<CacheScope, Set<String>> =
        mapOf(
            CacheScope.ARTICLE to
                setOf(
                    CacheNames.ARTICLES,
                    CacheNames.TRENDING_TICKER,
                ),
            CacheScope.GIT_REPO to
                setOf(
                    CacheNames.GIT_REPOS,
                    CacheNames.TRENDING_GIT_REPOS,
                    CacheNames.GIT_REPOS_BY_LANGUAGE,
                    CacheNames.GITHUB_TRENDING,
                ),
            CacheScope.LLM to
                setOf(
                    CacheNames.LLM_LEADERBOARD,
                    CacheNames.LLM_BENCHMARKS,
                    CacheNames.LLM_MODELS,
                ),
            CacheScope.UNKNOWN to
                setOf(
                    CacheNames.ARTICLES,
                    CacheNames.TRENDING_TICKER,
                    CacheNames.GIT_REPOS,
                    CacheNames.TRENDING_GIT_REPOS,
                    CacheNames.GIT_REPOS_BY_LANGUAGE,
                    CacheNames.GITHUB_TRENDING,
                    CacheNames.LLM_LEADERBOARD,
                    CacheNames.LLM_BENCHMARKS,
                    CacheNames.LLM_MODELS,
                ),
        )

    /** All cache names for the scope; empty set if unmapped. */
    fun forScope(scope: CacheScope): Set<String> = SCOPE_TO_CACHES[scope] ?: emptySet()
}
