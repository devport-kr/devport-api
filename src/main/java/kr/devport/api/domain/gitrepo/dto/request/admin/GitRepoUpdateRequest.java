package kr.devport.api.domain.gitrepo.dto.request.admin;

import kr.devport.api.domain.gitrepo.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitRepoUpdateRequest {

    private String fullName;
    private String url;
    private String description;
    private String language;
    private Integer stars;
    private Integer forks;
    private Integer starsThisWeek;
    private String summaryKoTitle;
    private String summaryKoBody;
    private Category category;
    private Integer score;
}
