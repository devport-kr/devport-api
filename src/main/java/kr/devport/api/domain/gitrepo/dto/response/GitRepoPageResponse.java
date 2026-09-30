package kr.devport.api.domain.gitrepo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Paginated git repository response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRepoPageResponse {

    @Schema(description = "List of git repositories")
    private List<GitRepoResponse> content;

    @Schema(description = "Total number of repositories", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "15")
    private int totalPages;

    @Schema(description = "Current page number", example = "0")
    private int currentPage;

    @Schema(description = "Whether there are more pages", example = "true")
    private boolean hasMore;
}
