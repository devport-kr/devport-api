package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Wiki snapshot response DTO aligned to dynamic crawler section contract.
 */
@Getter
@Builder
@Schema(description = "Project wiki snapshot with dynamic sections and readiness metadata")
public class WikiSnapshotResponse {

    @Schema(description = "Project external ID", example = "github:12345")
    private String projectExternalId;

    @Schema(description = "Snapshot generation timestamp")
    private OffsetDateTime generatedAt;

    @Schema(description = "Dynamic wiki sections with heading + anchor metadata")
    private List<WikiSectionResponse> sections;

    @Schema(description = "Current repository counters only (no star-history timeline)")
    private CurrentCountersResponse currentCounters;

    // Readiness and hiding controls
    @Schema(description = "Whether this project meets minimum data quality thresholds")
    private boolean isDataReady;

    @Schema(description = "Section IDs to hide due to incomplete or uncertain data")
    private List<String> hiddenSections;

    @Schema(description = "Detailed readiness scoring and gate results")
    private Map<String, Object> readinessMetadata;

    /**
     * Individual dynamic wiki section with summary + deep dive content.
     */
    @Getter
    @Builder
    @Schema(description = "Dynamic wiki section with progressive disclosure")
    public static class WikiSectionResponse {

        @Schema(description = "Stable section identifier", example = "architecture")
        private String sectionId;

        @Schema(description = "Human-readable section heading", example = "Architecture")
        private String heading;

        @Schema(description = "Anchor slug for navigation", example = "architecture")
        private String anchor;

        @Schema(description = "Short summary paragraph (1-3 sentences)", example = "Next.js is a React framework for production-grade web applications.")
        private String summary;

        @Schema(description = "Full technical explanation in markdown format")
        private String deepDiveMarkdown;

        @Schema(description = "Whether this section should be expanded by default")
        private boolean defaultExpanded;

        @Schema(description = "Optional Mermaid DSL for generated diagrams")
        private String generatedDiagramDsl;

        @Schema(description = "Optional section metadata")
        private Map<String, Object> metadata;
    }

    @Getter
    @Builder
    @Schema(description = "Current repository counters")
    public static class CurrentCountersResponse {

        @Schema(description = "Current star count", example = "12345")
        private Integer stars;

        @Schema(description = "Current fork count", example = "678")
        private Integer forks;

        @Schema(description = "Current watcher count", example = "321")
        private Integer watchers;

        @Schema(description = "Current open issue count", example = "54")
        private Integer openIssues;

        @Schema(description = "Timestamp when counters were refreshed")
        private OffsetDateTime updatedAt;
    }
}
