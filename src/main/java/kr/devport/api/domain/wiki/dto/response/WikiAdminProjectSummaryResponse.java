package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Admin wiki project summary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiAdminProjectSummaryResponse {

    @Schema(description = "Project internal ID", example = "101")
    private Long projectId;

    @Schema(description = "Project external ID", example = "github:owner/repo")
    private String projectExternalId;

    @Schema(description = "Repository full name", example = "owner/repo")
    private String fullName;

    @Schema(description = "Total stars", example = "125000")
    private Integer stars;

    @Schema(description = "Primary language", example = "TypeScript")
    private String language;

    @Schema(description = "Port display name", example = "Web")
    private String portName;

    @Schema(description = "Port slug", example = "web")
    private String portSlug;
}
