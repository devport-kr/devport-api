package kr.devport.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticlePageResponse {

    private List<ArticleResponse> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Boolean hasMore;
}
