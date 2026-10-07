package kr.devport.api.domain.common.config;

import kr.devport.api.domain.article.dto.response.ArticleMetadataResponse;
import kr.devport.api.domain.article.dto.response.ArticlePageResponse;
import kr.devport.api.domain.article.dto.response.ArticleResponse;
import kr.devport.api.domain.article.dto.response.TrendingTickerResponse;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoPageResponse;
import kr.devport.api.domain.gitrepo.dto.response.GitRepoResponse;
import kr.devport.api.domain.llm.dto.response.LLMBenchmarkResponse;
import kr.devport.api.domain.llm.dto.response.LLMLeaderboardEntryResponse;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Registers all DTO classes that need Jackson reflection for native image serialization.
 * Covers Redis @Cacheable return types and their nested classes.
 * Also imports Jakarta Mail content-handler hints (see {@link MailRuntimeHints}).
 */
@Configuration
@ImportRuntimeHints(MailRuntimeHints.class)
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
})
public class NativeImageReflectionConfig {
}
