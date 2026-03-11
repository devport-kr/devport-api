package kr.devport.api.domain.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Clears Spring-managed cache regions on startup without touching application state
 * stored directly in Redis (for example OAuth exchange codes or wiki sessions).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupCacheEvictor implements ApplicationRunner {

    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        List<String> cacheNames = List.of(
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
            CacheNames.WIKI_PROJECT_PAGE
        );

        int cleared = 0;
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Startup cache eviction skipped missing cache: {}", cacheName);
                continue;
            }
            cache.clear();
            cleared++;
        }

        log.info("Startup cache eviction completed for {} Spring cache regions: {}", cleared, cacheNames);
    }
}
