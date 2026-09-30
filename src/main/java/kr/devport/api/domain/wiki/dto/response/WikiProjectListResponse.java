package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Flat list of wiki-ready projects, sorted by stars descending.
 * Only projects with at least one wiki chunk are included.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Flat list of wiki-ready projects sorted by stars descending")
public class WikiProjectListResponse {

    @Schema(description = "List of wiki-ready projects")
    private List<ProjectSummary> projects;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Project summary for browse listing")
    public static class ProjectSummary {

        @Schema(description = "Project external ID", example = "github:owner/repo")
        private String projectExternalId;

        @Schema(description = "Repository full name", example = "owner/repo")
        private String fullName;

        @Schema(description = "Repository description")
        private String description;

        @Schema(description = "Star count")
        private Integer stars;

        @Schema(description = "Primary programming language")
        private String language;

        @Schema(description = "Content of first summary chunk")
        private String summary;
    }
}
