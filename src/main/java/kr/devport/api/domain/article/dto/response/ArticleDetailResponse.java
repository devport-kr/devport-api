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

@Schema(description = "Article detail response for article detail page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDetailResponse {

    @Schema(description = "Article external UUID for URL", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String externalId;

    @Schema(description = "Content item type", example = "BLOG")
    private ItemType itemType;

    @Schema(description = "Content source", example = "medium")
    private String source;

    @Schema(description = "Content category", example = "AI_LLM")
    private Category category;

    @Schema(description = "Korean title summary", example = "GPT-5의 새로운 기능들")
    private String summaryKoTitle;

    @Schema(description = "Korean body summary (Markdown content)", example = "## 주요 내용\n\nGPT-5가 발표되었습니다...")
    private String summaryKoBody;

    @Schema(description = "Original English title", example = "New Features in GPT-5")
    private String titleEn;

    @Schema(description = "Original article URL", example = "https://medium.com/original-article")
    private String url;

    @Schema(description = "Article score/popularity", example = "150")
    private Integer score;

    @Schema(description = "Article tags", example = "[\"AI\", \"LLM\", \"GPT\"]")
    private List<String> tags;

    @Schema(description = "Original creation timestamp", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAtSource;

    @Schema(description = "Additional metadata")
    private ArticleMetadataResponse metadata;
}
