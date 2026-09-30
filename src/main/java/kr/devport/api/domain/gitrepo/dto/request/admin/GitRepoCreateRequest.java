package kr.devport.api.domain.gitrepo.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.devport.api.domain.gitrepo.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitRepoCreateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "URL is required")
    private String url;

    private String description;

    private String language;

    private Integer stars;

    private Integer forks;

    private Integer starsThisWeek;

    private String summaryKoTitle;

    private String summaryKoBody;

    private Category category;

    @NotNull(message = "Score is required")
    @Builder.Default
    private Integer score = 0;
}
