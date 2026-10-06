package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.entity.ArticleMetadata;
import kr.devport.api.domain.article.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyDigestPickerTest {

    static Article article(long id, ItemType type, Integer upvotes, Integer comments) {
        return Article.builder()
            .id(id)
            .itemType(type)
            .source(type == ItemType.DISCUSSION ? "example.com" : "devto")
            .metadata(ArticleMetadata.builder().upvotes(upvotes).comments(comments).build())
            .build();
    }

    private static List<Long> ids(List<Article> articles) {
        return articles.stream().map(Article::getId).toList();
    }

    @Test
    void comparesWithinItemTypeSoBlogsAreNotDrownedOutByHackerNews() {
        List<Article> candidates = List.of(
            article(1, ItemType.DISCUSSION, 900, 300),
            article(2, ItemType.DISCUSSION, 700, 200),
            article(3, ItemType.DISCUSSION, 500, 100),
            article(4, ItemType.DISCUSSION, 400, 50),
            article(5, ItemType.BLOG, 40, 3),
            article(6, ItemType.BLOG, 25, 0));

        assertThat(ids(WeeklyDigestPicker.pick(candidates, 3, 2))).containsExactly(1L, 5L, 2L);
    }

    @Test
    void capsArticlesPerItemType() {
        List<Article> candidates = List.of(
            article(1, ItemType.DISCUSSION, 900, 0),
            article(2, ItemType.DISCUSSION, 800, 0),
            article(3, ItemType.DISCUSSION, 700, 0));

        assertThat(ids(WeeklyDigestPicker.pick(candidates, 3, 2))).containsExactly(1L, 2L);
    }

    @Test
    void commentsBreakCloseVoteCounts() {
        List<Article> candidates = List.of(
            article(1, ItemType.DISCUSSION, 300, 0),
            article(2, ItemType.DISCUSSION, 280, 120));

        assertThat(ids(WeeklyDigestPicker.pick(candidates, 1, 2))).containsExactly(2L);
    }

    @Test
    void skipsArticlesWithoutEngagement() {
        Article noMetadata = Article.builder().id(1L).itemType(ItemType.REPO).source("github").build();
        List<Article> candidates = List.of(
            noMetadata,
            article(2, ItemType.BLOG, 0, 0),
            article(3, ItemType.BLOG, 15, 1));

        assertThat(ids(WeeklyDigestPicker.pick(candidates, 3, 2))).containsExactly(3L);
    }

    @Test
    void emptyWeekPicksNothing() {
        assertThat(WeeklyDigestPicker.pick(List.of(), 3, 2)).isEmpty();
    }
}
