package kr.devport.api.domain.article.repository;

import kr.devport.api.domain.article.enums.Category;

/** Lightweight autocomplete row: only what the dropdown needs (no body, no tags). */
public record ArticleTitleSuggestionRow(
    String externalId,
    String summaryKoTitle,
    String source,
    Category category,
    Integer score
) {
}
