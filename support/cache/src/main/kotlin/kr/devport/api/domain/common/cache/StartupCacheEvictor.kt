package kr.devport.api.domain.common.cache

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * Clears Spring-managed cache regions on startup without touching application state stored directly
 * in Redis (e.g. OAuth exchange codes or wiki sessions).
 */
@Component
class StartupCacheEvictor(
    private val cacheManager: CacheManager,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val cacheNames =
            listOf(
                CacheNames.ARTICLES,
                CacheNames.TRENDING_TICKER,
                CacheNames.GIT_REPOS,
                CacheNames.TRENDING_GIT_REPOS,
                CacheNames.GIT_REPOS_BY_LANGUAGE,
                CacheNames.GITHUB_TRENDING,
                CacheNames.LLM_LEADERBOARD,
                CacheNames.LLM_BENCHMARKS,
                CacheNames.LLM_MODELS,
                CacheNames.WIKI_PROJECTS,
                CacheNames.WIKI_PROJECT_PAGE,
            )

        var cleared = 0
        for (cacheName in cacheNames) {
            val cache = cacheManager.getCache(cacheName)
            if (cache == null) {
                log.warn("Startup cache eviction skipped missing cache: {}", cacheName)
                continue
            }
            cache.clear()
            cleared++
        }

        log.info("Startup cache eviction completed for {} Spring cache regions: {}", cleared, cacheNames)
    }
}
