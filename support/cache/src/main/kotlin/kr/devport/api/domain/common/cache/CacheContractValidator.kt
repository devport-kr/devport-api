package kr.devport.api.domain.common.cache

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * Fails application startup if any required cache from [CacheNames] is missing from the CacheManager,
 * preventing silent cache-name drift.
 */
@Component
class CacheContractValidator(
    private val cacheManager: CacheManager,
) {
    @PostConstruct
    fun validateCacheContract() {
        val requiredCaches = requiredCacheNames()
        val initializedCaches = cacheManager.cacheNames

        val missingCaches = requiredCaches.filterNot { initializedCaches.contains(it) }.toSet()

        if (missingCaches.isNotEmpty()) {
            val errorMessage =
                "Cache contract violation: Required caches are not initialized in CacheManager: $missingCaches. " +
                    "Initialized caches: $initializedCaches. " +
                    "Please ensure all CacheNames constants have corresponding TTL configuration."
            log.error(errorMessage)
            throw IllegalStateException(errorMessage)
        }

        log.info(
            "Cache contract validation passed. All {} required caches are initialized: {}",
            requiredCaches.size,
            requiredCaches,
        )
    }

    private fun requiredCacheNames(): Set<String> =
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
            CacheNames.WIKI_PROJECTS,
            CacheNames.WIKI_PROJECT_PAGE,
        )

    companion object {
        private val log = LoggerFactory.getLogger(CacheContractValidator::class.java)
    }
}
