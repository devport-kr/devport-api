package kr.devport.api.domain.common.config;

import kr.devport.api.domain.article.dto.response.ArticleMetadataResponse;
import kr.devport.api.domain.article.dto.response.ArticlePageResponse;
import kr.devport.api.domain.article.dto.response.ArticleResponse;
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoPageResponse;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoResponse;
import kr.devport.api.domain.llm.dto.response.LLMBenchmarkResponse;
import kr.devport.api.domain.llm.dto.response.LLMLeaderboardEntryResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all DTO classes that need Jackson reflection for native image serialization.
 * Covers Redis @Cacheable return types and their nested classes.
 */
@Configuration
@RegisterReflectionForBinding({
        // Article cache DTOs
        ArticlePageResponse.class,
        ArticleResponse.class,
        ArticleMetadataResponse.class,
        TrendingTickerResponse.class,
        // GitRepo cache DTOs
        GitRepoPageResponse.class,
        GitRepoResponse.class,
        // LLM cache DTOs
        LLMLeaderboardEntryResponse.class,
        LLMBenchmarkResponse.class,
        // Wiki cache DTOs
        WikiProjectListResponse.class,
        WikiProjectListResponse.ProjectSummary.class,
        WikiProjectPageResponse.class,
        WikiProjectPageResponse.WikiSection.class,
        WikiProjectPageResponse.AnchorItem.class,
        WikiProjectPageResponse.DiagramMetadata.class,
        WikiProjectPageResponse.CurrentCounters.class,
        WikiProjectPageResponse.RightRailOrdering.class,
        // Redis session types
        WikiChatSessionStore.ChatSession.class,
        WikiChatSessionStore.ChatTurn.class,
})
public class NativeImageReflectionConfig {
}
