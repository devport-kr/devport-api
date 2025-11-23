package kr.devport.api.dto.response;

import kr.devport.api.domain.enums.Category;
import kr.devport.api.domain.enums.ItemType;
import kr.devport.api.domain.enums.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {

    private Long id;
    private ItemType itemType;
    private Source source;
    private Category category;
    private String summaryKoTitle;
    private String summaryKoBody;
    private String titleEn;
    private String url;
    private Integer score;
    private List<String> tags;
    private LocalDateTime createdAtSource;
    private ArticleMetadataResponse metadata;
}
