package kr.devport.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Paginated article response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticlePageResponse {

    @Schema(description = "List of articles on current page")
    private List<ArticleResponse> content;

    @Schema(description = "Total number of articles", example = "150")
    private Long totalElements;

    @Schema(description = "Total number of pages", example = "17")
    private Integer totalPages;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private Integer currentPage;

    @Schema(description = "Whether there are more pages", example = "true")
    private Boolean hasMore;
}
