package kr.devport.api.domain.article.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleLLMPreviewResponse {

    private boolean isTechnical;
    private String titleKo;
    private String summaryKo;
    private String category;
    private List<String> tags;
    private String url;
    private String titleEn;
    private String source;
}
