package kr.devport.api.domain.article.dto.request;

import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchCondition {

    // Existing filters
    private Category category;
    private String source;

    // New filters for enhanced search
    private ItemType itemType;
    private String keyword;              // title search (Korean or English)
    private Integer minScore;
    private Integer maxScore;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private List<String> tags;           // ANY match
}
