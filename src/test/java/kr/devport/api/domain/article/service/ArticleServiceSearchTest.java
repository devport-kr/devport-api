package kr.devport.api.domain.article.service;

import kr.devport.api.domain.article.dto.response.ArticleAutocompleteListResponse;
import kr.devport.api.domain.article.dto.response.ArticleAutocompleteResponse;
import kr.devport.api.domain.article.dto.response.ArticlePageResponse;
import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.repository.ArticleRepository;
import kr.devport.api.domain.article.repository.ArticleTitleSuggestionRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleServiceSearchTest {

    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final ArticleService service = new ArticleService(repository);

    private final ArticleTitleSuggestionRow row =
        new ArticleTitleSuggestionRow("ext-1", "React 19 released", "hackernews", Category.AI_LLM, 10);

    @Test
    @DisplayName("autocomplete reports the exact count when under the cap")
    void autocompleteUnderCap() {
        when(repository.findTitleSuggestions("react", 5)).thenReturn(List.of(row));
        when(repository.countTitleMatches("react", 101)).thenReturn(42L);

        ArticleAutocompleteListResponse response = service.searchAutocomplete("react");

        assertThat(response.getTotalMatches()).isEqualTo(42L);
        assertThat(response.getTotalCapped()).isFalse();
        assertThat(response.getSuggestions()).hasSize(1);
        assertThat(response.getSuggestions().get(0).getMatchType())
            .isEqualTo(ArticleAutocompleteResponse.MatchType.TITLE);
    }

    @Test
    @DisplayName("autocomplete caps totalMatches at 100 and flags it")
    void autocompleteOverCap() {
        when(repository.findTitleSuggestions("react", 5)).thenReturn(List.of(row));
        when(repository.countTitleMatches("react", 101)).thenReturn(101L);

        ArticleAutocompleteListResponse response = service.searchAutocomplete("react");

        assertThat(response.getTotalMatches()).isEqualTo(100L);
        assertThat(response.getTotalCapped()).isTrue();
    }

    @Test
    @DisplayName("exactly 100 matches is not flagged as capped")
    void autocompleteExactlyAtCap() {
        when(repository.findTitleSuggestions("react", 5)).thenReturn(List.of(row));
        when(repository.countTitleMatches("react", 101)).thenReturn(100L);

        ArticleAutocompleteListResponse response = service.searchAutocomplete("react");

        assertThat(response.getTotalMatches()).isEqualTo(100L);
        assertThat(response.getTotalCapped()).isFalse();
    }

    @Test
    @DisplayName("fulltext page math below the cap")
    void fulltextBelowCap() {
        when(repository.searchFulltextContent(any(), any())).thenReturn(List.of());
        when(repository.countFulltextMatches("ai", 1001)).thenReturn(45L);

        ArticlePageResponse response = service.searchFulltext("ai", 1, 20);

        assertThat(response.getTotalElements()).isEqualTo(45L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getHasMore()).isTrue();
        assertThat(response.getTotalCapped()).isFalse();
    }

    @Test
    @DisplayName("fulltext last page has no more")
    void fulltextLastPage() {
        when(repository.searchFulltextContent(any(), any())).thenReturn(List.of());
        when(repository.countFulltextMatches("ai", 1001)).thenReturn(45L);

        assertThat(service.searchFulltext("ai", 2, 20).getHasMore()).isFalse();
    }

    @Test
    @DisplayName("fulltext caps totalElements at 1000 and flags it")
    void fulltextOverCap() {
        when(repository.searchFulltextContent(any(), any())).thenReturn(List.of());
        when(repository.countFulltextMatches("ai", 1001)).thenReturn(1001L);

        ArticlePageResponse response = service.searchFulltext("ai", 0, 20);

        assertThat(response.getTotalElements()).isEqualTo(1000L);
        assertThat(response.getTotalPages()).isEqualTo(50);
        assertThat(response.getTotalCapped()).isTrue();
        assertThat(response.getHasMore()).isTrue();
    }
}
