package kr.devport.api.domain.common.cache;

/**
 * Single source of truth for all cache names used across the application.
 * All @Cacheable, @CacheEvict, and programmatic cache operations must reference these constants.
 */
public final class CacheNames {
    
    // Article domain caches
    public static final String ARTICLES = "articles";
    public static final String TRENDING_TICKER = "trendingTicker";
    
    // Git repository domain caches
    public static final String GIT_REPOS = "gitRepos";
    public static final String TRENDING_GIT_REPOS = "trendingGitRepos";
    public static final String GIT_REPOS_BY_LANGUAGE = "gitReposByLanguage";
    public static final String GITHUB_TRENDING = "githubTrending";
    
    // LLM domain caches
    public static final String LLM_LEADERBOARD = "llmLeaderboard";
    public static final String LLM_BENCHMARKS = "benchmarks";
    public static final String LLM_MODELS = "llmModels";

    // Wiki domain caches
    public static final String WIKI_PROJECTS = "wikiProjects";
    public static final String WIKI_PROJECT_PAGE = "wikiProjectPage";
    
    private CacheNames() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
