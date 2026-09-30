package kr.devport.api.domain.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.mypage.entity.UserSavedArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Saved article response")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedArticleResponse {

    @Schema(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String articleId;

    @Schema(description = "Korean title summary", example = "React 19의 새로운 기능들")
    private String summaryKoTitle;

    @Schema(description = "Content source", example = "medium")
    private String source;

    @Schema(description = "Content category", example = "FRONTEND")
    private String category;

    @Schema(description = "Article URL", example = "https://medium.com/article")
    private String url;

    @Schema(description = "When the article was saved", example = "2025-02-03T10:00:00")
    private LocalDateTime savedAt;

    public static SavedArticleResponse from(UserSavedArticle saved) {
        Article article = saved.getArticle();
        return SavedArticleResponse.builder()
            .articleId(article.getExternalId())
            .summaryKoTitle(article.getSummaryKoTitle())
            .source(article.getSource())
            .category(article.getCategory().name())
            .url(article.getUrl())
            .savedAt(saved.getCreatedAt())
            .build();
    }
}
