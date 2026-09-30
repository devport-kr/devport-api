package kr.devport.api.domain.port.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectCreateRequest {

    /** GitHub full name, e.g. "owner/repo" */
    @NotBlank
    @Pattern(regexp = ".+/.+", message = "fullName must be in owner/repo format")
    private String fullName;

    private String repoUrl;

    private String homepageUrl;
    private String description;
    private Integer stars;
    private Integer forks;
    private String language;
    private String license;
}
