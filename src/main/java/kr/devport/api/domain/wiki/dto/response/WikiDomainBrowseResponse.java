package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for domain-based wiki browsing.
 * Returns top-starred data-ready projects grouped by domain.
 */
@Getter
@Builder
@Schema(description = "Domain browse response with top-starred wiki-ready projects")
public class WikiDomainBrowseResponse {

    @Schema(description = "Domain name", example = "web-frameworks")
    private String domain;

    @Schema(description = "List of wiki-ready projects in this domain, sorted by stars descending")
    private List<ProjectSummary> projects;

    /**
     * Project summary for domain browse listing.
     */
    @Getter
    @Builder
    @Schema(description = "Project summary for browse listing")
    public static class ProjectSummary {

        @Schema(description = "Project external ID", example = "github:12345")
        private String projectExternalId;

        @Schema(description = "Repository full name", example = "vercel/next.js")
        private String fullName;

        @Schema(description = "Repository description")
        private String description;

        @Schema(description = "Star count")
        private Integer stars;

        @Schema(description = "Primary programming language")
        private String language;

        @Schema(description = "Short summary (1 sentence)")
        private String summary;
    }
}
