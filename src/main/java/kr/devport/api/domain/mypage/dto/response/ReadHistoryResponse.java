package kr.devport.api.domain.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.mypage.entity.UserReadHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Read history response")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadHistoryResponse {

    @Schema(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String articleId;

    @Schema(description = "Korean title summary", example = "Kubernetes 모범 사례")
    private String summaryKoTitle;

    @Schema(description = "Content source", example = "hackernews")
    private String source;

    @Schema(description = "Content category", example = "DEVOPS_SRE")
    private String category;

    @Schema(description = "Article URL", example = "https://hackernews.com/article")
    private String url;

    @Schema(description = "When the article was read", example = "2025-02-03T09:30:00")
    private LocalDateTime readAt;

    public static ReadHistoryResponse from(UserReadHistory history) {
        Article article = history.getArticle();
        return ReadHistoryResponse.builder()
            .articleId(article.getExternalId())
            .summaryKoTitle(article.getSummaryKoTitle())
            .source(article.getSource())
            .category(article.getCategory().name())
            .url(article.getUrl())
            .readAt(history.getReadAt())
            .build();
    }
}
