package kr.devport.api.domain.article.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** LIKE-pattern building for the JdbcTemplate search queries (no database needed). */
class ArticleSearchSqlPatternTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ArticleRepositoryImpl repository = new ArticleRepositoryImpl(mock(JPAQueryFactory.class), jdbcTemplate);

    private String capturedTitlePattern(String query) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), org.mockito.ArgumentMatchers.<Object[]>any()))
            .thenReturn(0L);
        repository.countTitleMatches(query, 101);
        ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), args.capture(), args.capture());
        return (String) args.getAllValues().get(0);
    }

    @Test
    @DisplayName("term is trimmed and wrapped in %")
    void wrapsAndTrims() {
        assertThat(capturedTitlePattern("  react ")).isEqualTo("%react%");
    }

    @Test
    @DisplayName("LIKE metacharacters are escaped with '!'")
    void escapesMetacharacters() {
        assertThat(capturedTitlePattern("100%_a!")).isEqualTo("%100!%!_a!!%");
    }

    @Test
    @DisplayName("queries under 2 characters never touch the database")
    void shortQueryIsNoop() {
        assertThat(repository.countTitleMatches("a", 101)).isZero();
        assertThat(repository.findTitleSuggestions(" a ", 5)).isEmpty();
        assertThat(repository.countFulltextMatches("a", 1001)).isZero();
        verifyNoInteractions(jdbcTemplate);
    }
}
