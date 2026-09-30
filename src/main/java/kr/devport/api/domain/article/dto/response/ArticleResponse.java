package kr.devport.api.domain.article.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Article/Content response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {

    @Schema(description = "Article ID", example = "1")
    private Long id;

    @Schema(description = "Article external UUID for URL", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String externalId;

    @Schema(description = "Content item type", example = "ARTICLE")
    private ItemType itemType;

    @Schema(description = "Content source", example = "github")
    private String source;

    @Schema(description = "Content category", example = "AI_LLM")
    private Category category;

    @Schema(description = "Korean title summary", example = "새로운 AI 모델 공개")
    private String summaryKoTitle;

    @Schema(description = "Korean body summary", example = "OpenAI가 새로운 언어 모델을 공개했습니다")
    private String summaryKoBody;

    @Schema(description = "Original English title", example = "New AI Model Released")
    private String titleEn;

    @Schema(description = "Article URL", example = "https://github.com/example/repo")
    private String url;

    @Schema(description = "Article score/popularity", example = "1250")
    private Integer score;

    @Schema(description = "Article tags", example = "[\"AI\", \"Machine Learning\", \"NLP\"]")
    private List<String> tags;

    @Schema(description = "Original creation timestamp", example = "2025-01-15T10:00:00")
    private LocalDateTime createdAtSource;

    @Schema(description = "Additional metadata")
    private ArticleMetadataResponse metadata;
}
