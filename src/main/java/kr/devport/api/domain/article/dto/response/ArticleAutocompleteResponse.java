package kr.devport.api.domain.article.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Lightweight article autocomplete suggestion")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAutocompleteResponse {

    @Schema(description = "Article external UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String externalId;

    @Schema(description = "Korean title summary", example = "React 19의 새로운 기능들")
    private String summaryKoTitle;

    @Schema(description = "Content source", example = "medium")
    private String source;

    @Schema(description = "Content category", example = "FRONTEND")
    private String category;

    @Schema(description = "Match type for UI highlighting", example = "TITLE")
    private MatchType matchType;

    @Schema(description = "Article score/popularity", example = "1250")
    private Integer score;

    public enum MatchType {
        TITLE,
        BODY
    }
}
