package kr.devport.api.domain.common.cache

/**
 * Single source of truth for all cache names used across the application. All @Cacheable,
 * @CacheEvict, and programmatic cache operations must reference these constants.
 */
object CacheNames {
    // Article domain caches
    const val ARTICLES = "articles"
    const val TRENDING_TICKER = "trendingTicker"

    // Git repository domain caches
    const val GIT_REPOS = "gitRepos"
    const val TRENDING_GIT_REPOS = "trendingGitRepos"
    const val GIT_REPOS_BY_LANGUAGE = "gitReposByLanguage"
    const val GITHUB_TRENDING = "githubTrending"

    // LLM domain caches
    const val LLM_LEADERBOARD = "llmLeaderboard"
    const val LLM_BENCHMARKS = "benchmarks"
    const val LLM_MODELS = "llmModels"

    // Wiki domain caches
    const val WIKI_PROJECTS = "wikiProjects"
    const val WIKI_PROJECT_PAGE = "wikiProjectPage"
}
