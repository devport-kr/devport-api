package kr.devport.api.domain.article.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateRequest {

    @NotNull(message = "Item type is required")
    private ItemType itemType;

    @NotBlank(message = "Source is required")
    private String source;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Korean summary title is required")
    private String summaryKoTitle;

    private String summaryKoBody;

    @NotBlank(message = "English title is required")
    private String titleEn;

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Score is required")
    private Integer score;

    private List<String> tags;

    @NotNull(message = "Source creation date is required")
    private LocalDateTime createdAtSource;

    private ArticleMetadataRequest metadata;
}
