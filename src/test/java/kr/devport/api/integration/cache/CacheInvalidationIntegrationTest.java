package kr.devport.api.integration.cache;

import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.common.cache.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cache invalidation correctness.
 * 
 * Tests verify:
 * - Scoped invalidation clears related cache groups
 * - Different key dimensions create separate cache entries (no collision)
 * - Scope isolation (invalidating one scope doesn't affect others)
 * - Unknown scope safety (broad invalidation)
 * 
 * Uses in-memory CacheManager for deterministic testing without external dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cache Invalidation Integration Tests")
class CacheInvalidationIntegrationTest {

    private final CacheKeyFactory CacheKeyFactory = new CacheKeyFactory();
    private CacheManager cacheManager;
    private CacheFallbackStateStore fallbackStateStore;
    private CacheInvalidationService invalidationService;
    
    @BeforeEach
    void setUp() {
        // Use concurrent map cache manager (in-memory, deterministic)
        cacheManager = new ConcurrentMapCacheManager(
            CacheNames.ARTICLES,
            CacheNames.TRENDING_TICKER,
            CacheNames.GIT_REPOS,
            CacheNames.TRENDING_GIT_REPOS,
            CacheNames.GIT_REPOS_BY_LANGUAGE,
            CacheNames.GITHUB_TRENDING,
            CacheNames.LLM_LEADERBOARD,
            CacheNames.LLM_BENCHMARKS,
            CacheNames.LLM_MODELS
        );
        
        fallbackStateStore = new CacheFallbackStateStore();
        invalidationService = new CacheInvalidationService(cacheManager, fallbackStateStore);
    }
    
    @Test
    @DisplayName("Article scope invalidation clears all article-related caches")
    void articleScope_invalidation_clearsArticleCaches() {
        // Given: Populate article caches
        String articleListKey = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
        String trendingTickerKey = CacheKeyFactory.trendingTickerKey(10);
        
        Cache articlesCache = cacheManager.getCache(CacheNames.ARTICLES);
        Cache tickerCache = cacheManager.getCache(CacheNames.TRENDING_TICKER);
        
        articlesCache.put(articleListKey, "cached-article-list");
        tickerCache.put(trendingTickerKey, "cached-ticker");
        
        // Verify caches are populated
        assertThat(articlesCache.get(articleListKey)).isNotNull();
        assertThat(tickerCache.get(trendingTickerKey)).isNotNull();
        
        // When: Invalidate article scope
        invalidationService.invalidateScope(CacheScope.ARTICLE, "job-article-123");
        
        // Then: Article caches are cleared
        assertThat(articlesCache.get(articleListKey))
            .as("Articles cache should be cleared after invalidation")
            .isNull();
        assertThat(tickerCache.get(trendingTickerKey))
            .as("Trending ticker cache should be cleared after invalidation")
            .isNull();
    }
    
    @Test
    @DisplayName("GitRepo scope invalidation clears all git-repo-related caches")
    void gitRepoScope_invalidation_clearsGitRepoCaches() {
        // Given: Populate git repo caches
        String gitRepoListKey = CacheKeyFactory.gitRepoListKey(
            kr.devport.api.domain.gitrepo.enums.Category.BACKEND, 0, 20);
        String languageKey = CacheKeyFactory.gitReposByLanguageKey("java", 10);
        
        Cache gitReposCache = cacheManager.getCache(CacheNames.GIT_REPOS);
        Cache languageCache = cacheManager.getCache(CacheNames.GIT_REPOS_BY_LANGUAGE);
        
        gitReposCache.put(gitRepoListKey, "cached-repo-list");
        languageCache.put(languageKey, "cached-language-list");
        
        // Verify caches are populated
        assertThat(gitReposCache.get(gitRepoListKey)).isNotNull();
        assertThat(languageCache.get(languageKey)).isNotNull();
        
        // When: Invalidate git repo scope
        invalidationService.invalidateScope(CacheScope.GIT_REPO, "job-gitrepo-456");
        
        // Then: Git repo caches are cleared
        assertThat(gitReposCache.get(gitRepoListKey))
            .as("Git repos cache should be cleared after invalidation")
            .isNull();
        assertThat(languageCache.get(languageKey))
            .as("Language-filtered cache should be cleared after invalidation")
            .isNull();
    }
    
    @Test
    @DisplayName("LLM scope invalidation clears all LLM-related caches")
    void llmScope_invalidation_clearsLlmCaches() {
        // Given: Populate LLM caches
        String leaderboardKey = CacheKeyFactory.llmLeaderboardKey(
            kr.devport.api.domain.llm.enums.BenchmarkType.GPQA_DIAMOND,
            null, null, null, null, null);
        String benchmarksKey = CacheKeyFactory.allBenchmarksKey();
        
        Cache leaderboardCache = cacheManager.getCache(CacheNames.LLM_LEADERBOARD);
        Cache benchmarksCache = cacheManager.getCache(CacheNames.LLM_BENCHMARKS);
        
        leaderboardCache.put(leaderboardKey, "cached-leaderboard");
        benchmarksCache.put(benchmarksKey, "cached-benchmarks");
        
        // Verify caches are populated
        assertThat(leaderboardCache.get(leaderboardKey)).isNotNull();
        assertThat(benchmarksCache.get(benchmarksKey)).isNotNull();
        
        // When: Invalidate LLM scope
        invalidationService.invalidateScope(CacheScope.LLM, "job-llm-789");
        
        // Then: LLM caches are cleared
        assertThat(leaderboardCache.get(leaderboardKey))
            .as("LLM leaderboard cache should be cleared after invalidation")
            .isNull();
        assertThat(benchmarksCache.get(benchmarksKey))
            .as("Benchmarks cache should be cleared after invalidation")
            .isNull();
    }
    
    @Test
    @DisplayName("Unknown scope triggers broad invalidation of all critical caches")
    void unknownScope_invalidation_clearsAllCriticalCaches() {
        // Given: Populate caches across all scopes
        String articleKey = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
        String gitRepoKey = CacheKeyFactory.gitRepoListKey(
            kr.devport.api.domain.gitrepo.enums.Category.BACKEND, 0, 20);
        String llmKey = CacheKeyFactory.llmLeaderboardKey(
            kr.devport.api.domain.llm.enums.BenchmarkType.GPQA_DIAMOND,
            null, null, null, null, null);
        
        Cache articlesCache = cacheManager.getCache(CacheNames.ARTICLES);
        Cache gitReposCache = cacheManager.getCache(CacheNames.GIT_REPOS);
        Cache leaderboardCache = cacheManager.getCache(CacheNames.LLM_LEADERBOARD);
        
        articlesCache.put(articleKey, "cached-articles");
        gitReposCache.put(gitRepoKey, "cached-repos");
        leaderboardCache.put(llmKey, "cached-leaderboard");
        
        // Verify all caches are populated
        assertThat(articlesCache.get(articleKey)).isNotNull();
        assertThat(gitReposCache.get(gitRepoKey)).isNotNull();
        assertThat(leaderboardCache.get(llmKey)).isNotNull();
        
        // When: Invalidate UNKNOWN scope
        invalidationService.invalidateScope(CacheScope.UNKNOWN, "job-unknown-999");
        
        // Then: All critical caches are cleared for safety
        assertThat(articlesCache.get(articleKey))
            .as("Articles cache should be cleared for unknown scope")
            .isNull();
        assertThat(gitReposCache.get(gitRepoKey))
            .as("Git repos cache should be cleared for unknown scope")
            .isNull();
        assertThat(leaderboardCache.get(llmKey))
            .as("LLM leaderboard cache should be cleared for unknown scope")
            .isNull();
    }
    
    @Test
    @DisplayName("Different cache key dimensions create separate cache entries without collision")
    void differentKeyDimensions_createSeparateEntries_noCollision() {
        // Given: Different article queries
        String keyCategory1 = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
        String keyCategory2 = CacheKeyFactory.articleListKey(Category.FRONTEND, 0, 20);
        String keyPage1 = CacheKeyFactory.articleListKey(Category.AI_LLM, 1, 20);
        
        Cache articlesCache = cacheManager.getCache(CacheNames.ARTICLES);
        
        // Populate with different values
        articlesCache.put(keyCategory1, "ai-llm-page-0");
        articlesCache.put(keyCategory2, "frontend-page-0");
        articlesCache.put(keyPage1, "ai-llm-page-1");
        
        // Then: Each key maintains separate cached value (no collision)
        assertThat(articlesCache.get(keyCategory1, String.class))
            .isEqualTo("ai-llm-page-0");
        assertThat(articlesCache.get(keyCategory2, String.class))
            .isEqualTo("frontend-page-0");
        assertThat(articlesCache.get(keyPage1, String.class))
            .isEqualTo("ai-llm-page-1");
        
        // Verify keys are distinct
        assertThat(keyCategory1).isNotEqualTo(keyCategory2);
        assertThat(keyCategory1).isNotEqualTo(keyPage1);
        assertThat(keyCategory2).isNotEqualTo(keyPage1);
    }
    
    @Test
    @DisplayName("Scoped invalidation only affects related caches, not other scopes")
    void scopedInvalidation_onlyAffectsRelatedCaches() {
        // Given: Populate caches for multiple scopes
        String articleKey = CacheKeyFactory.articleListKey(Category.AI_LLM, 0, 20);
        String gitRepoKey = CacheKeyFactory.gitRepoListKey(
            kr.devport.api.domain.gitrepo.enums.Category.BACKEND, 0, 20);
        String llmKey = CacheKeyFactory.llmLeaderboardKey(
            kr.devport.api.domain.llm.enums.BenchmarkType.GPQA_DIAMOND,
            null, null, null, null, null);
        
        Cache articlesCache = cacheManager.getCache(CacheNames.ARTICLES);
        Cache gitReposCache = cacheManager.getCache(CacheNames.GIT_REPOS);
        Cache leaderboardCache = cacheManager.getCache(CacheNames.LLM_LEADERBOARD);
        
        articlesCache.put(articleKey, "cached-articles");
        gitReposCache.put(gitRepoKey, "cached-repos");
        leaderboardCache.put(llmKey, "cached-leaderboard");
        
        // When: Invalidate only ARTICLE scope
        invalidationService.invalidateScope(CacheScope.ARTICLE, "job-article-selective");
        
        // Then: Only article caches are cleared, others remain
        assertThat(articlesCache.get(articleKey))
            .as("Article cache should be cleared")
            .isNull();
        assertThat(gitReposCache.get(gitRepoKey))
            .as("Git repo cache should remain (different scope)")
            .isNotNull();
        assertThat(leaderboardCache.get(llmKey))
            .as("LLM cache should remain (different scope)")
            .isNotNull();
    }
}
