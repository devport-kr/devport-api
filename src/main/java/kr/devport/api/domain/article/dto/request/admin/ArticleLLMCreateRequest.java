package kr.devport.api.domain.article.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import kr.devport.api.domain.article.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleLLMCreateRequest {

    @NotBlank(message = "English title is required")
    private String titleEn;

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Source is required")
    private String source;

    @Builder.Default
    private ItemType itemType = ItemType.BLOG;

    private List<String> tags;

    private ArticleMetadataRequest metadata;
}
