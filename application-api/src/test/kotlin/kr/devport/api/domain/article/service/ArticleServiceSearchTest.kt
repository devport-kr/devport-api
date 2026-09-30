package kr.devport.api.domain.article.service

import kr.devport.api.domain.article.enums.Category
import kr.devport.api.domain.article.infrastructure.ArticleRepository
import kr.devport.api.domain.article.infrastructure.ArticleTitleSuggestionRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ArticleServiceSearchTest {
    private val repository: ArticleRepository = mock()
    private val service = ArticleService(repository)

    private val row = ArticleTitleSuggestionRow("ext-1", "React 19 released", "hackernews", Category.AI_LLM, 10)

    @Test
    @DisplayName("autocomplete reports the exact count when under the cap")
    fun autocompleteUnderCap() {
        whenever(repository.findTitleSuggestions("react", 5)).thenReturn(listOf(row))
        whenever(repository.countTitleMatches("react", 101)).thenReturn(42L)

        val response = service.searchAutocomplete("react")

        assertThat(response.totalMatches).isEqualTo(42L)
        assertThat(response.totalCapped).isFalse()
        assertThat(response.suggestions).hasSize(1)
        assertThat(
            response.suggestions!!.first().matchType,
        ).isEqualTo(kr.devport.api.domain.article.dto.response.ArticleAutocompleteResponse.MatchType.TITLE)
    }

    @Test
    @DisplayName("autocomplete caps totalMatches at 100 and flags it")
    fun autocompleteOverCap() {
        whenever(repository.findTitleSuggestions("react", 5)).thenReturn(listOf(row))
        whenever(repository.countTitleMatches("react", 101)).thenReturn(101L)

        val response = service.searchAutocomplete("react")

        assertThat(response.totalMatches).isEqualTo(100L)
        assertThat(response.totalCapped).isTrue()
    }

    @Test
    @DisplayName("exactly 100 matches is not flagged as capped")
    fun autocompleteExactlyAtCap() {
        whenever(repository.findTitleSuggestions("react", 5)).thenReturn(listOf(row))
        whenever(repository.countTitleMatches("react", 101)).thenReturn(100L)

        val response = service.searchAutocomplete("react")

        assertThat(response.totalMatches).isEqualTo(100L)
        assertThat(response.totalCapped).isFalse()
    }

    @Test
    @DisplayName("fulltext page math below the cap")
    fun fulltextBelowCap() {
        whenever(repository.searchFulltextContent(any(), any())).thenReturn(emptyList())
        whenever(repository.countFulltextMatches("ai", 1001)).thenReturn(45L)

        val response = service.searchFulltext("ai", 1, 20)

        assertThat(response.totalElements).isEqualTo(45L)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.currentPage).isEqualTo(1)
        assertThat(response.hasMore).isTrue() // page 1 of 0..2
        assertThat(response.totalCapped).isFalse()
    }

    @Test
    @DisplayName("fulltext last page has no more")
    fun fulltextLastPage() {
        whenever(repository.searchFulltextContent(any(), any())).thenReturn(emptyList())
        whenever(repository.countFulltextMatches("ai", 1001)).thenReturn(45L)

        assertThat(service.searchFulltext("ai", 2, 20).hasMore).isFalse()
    }

    @Test
    @DisplayName("fulltext caps totalElements at 1000 and flags it")
    fun fulltextOverCap() {
        whenever(repository.searchFulltextContent(any(), any())).thenReturn(emptyList())
        whenever(repository.countFulltextMatches("ai", 1001)).thenReturn(1001L)

        val response = service.searchFulltext("ai", 0, 20)

        assertThat(response.totalElements).isEqualTo(1000L)
        assertThat(response.totalPages).isEqualTo(50)
        assertThat(response.totalCapped).isTrue()
        assertThat(response.hasMore).isTrue()
    }
}
