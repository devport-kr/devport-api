package kr.devport.api.domain.article.dto.request.admin;

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
public class ArticleUpdateRequest {

    private ItemType itemType;
    private String source;
    private Category category;
    private String summaryKoTitle;
    private String summaryKoBody;
    private String titleEn;
    private String url;
    private Integer score;
    private List<String> tags;
    private LocalDateTime createdAtSource;
    private ArticleMetadataRequest metadata;
}
