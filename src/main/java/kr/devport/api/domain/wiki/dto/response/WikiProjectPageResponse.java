package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for project wiki page with dynamic sections and anchor navigation.
 */
@Getter
@Builder
@Schema(description = "Project wiki page with dynamic sections, anchors, and current counters")
public class WikiProjectPageResponse {

    @Schema(description = "Project external ID", example = "github:12345")
    private String projectExternalId;

    @Schema(description = "Repository full name", example = "vercel/next.js")
    private String fullName;

    @Schema(description = "Snapshot generation timestamp")
    private OffsetDateTime generatedAt;

    @Schema(description = "Visible dynamic wiki sections")
    private List<WikiSection> sections;

    @Schema(description = "Anchor navigation entries derived from visible sections")
    private List<AnchorItem> anchors;

    @Schema(description = "Hidden section IDs due to incomplete/uncertain content")
    private List<String> hiddenSections;

    @Schema(description = "Current repository counters only")
    private CurrentCounters currentCounters;

    // Right-rail ordering payload
    @Schema(description = "Right-rail modules in priority order: activity, releases, chat")
    private RightRailOrdering rightRail;

    /**
     * Dynamic wiki section with progressive disclosure.
     */
    @Getter
    @Builder
    @Schema(description = "Dynamic wiki section")
    public static class WikiSection {

        @Schema(description = "Stable section identifier", example = "architecture")
        private String sectionId;

        @Schema(description = "Section heading", example = "Architecture")
        private String heading;

        @Schema(description = "Section anchor slug", example = "architecture")
        private String anchor;

        @Schema(description = "Short summary paragraph (1-3 sentences)")
        private String summary;

        @Schema(description = "Full technical explanation in markdown format")
        private String deepDiveMarkdown;

        @Schema(description = "Whether this section should be expanded by default")
        private boolean defaultExpanded;
        @Schema(description = "Generated diagram DSL (Mermaid) when available")
        private String generatedDiagramDsl;

        @Schema(description = "Diagram metadata when diagram is available")
        private DiagramMetadata diagramMetadata;

        @Schema(description = "Optional section metadata")
        private Map<String, Object> metadata;
    }

    @Getter
    @Builder
    @Schema(description = "Anchor item for left-rail navigation")
    public static class AnchorItem {

        @Schema(description = "Section identifier", example = "architecture")
        private String sectionId;

        @Schema(description = "Section heading", example = "Architecture")
        private String heading;

        @Schema(description = "Section anchor slug", example = "architecture")
        private String anchor;
    }

    /**
     * Diagram metadata for rendering hints and accessibility.
     */
    @Getter
    @Builder
    @Schema(description = "Diagram metadata and render hints")
    public static class DiagramMetadata {

        @Schema(description = "Diagram type", example = "component-flow")
        private String diagramType;

        @Schema(description = "Accessible diagram description for screen readers")
        private String altText;

        @Schema(description = "Mermaid renderer config hints")
        private String renderHints;
    }

    @Getter
    @Builder
    @Schema(description = "Current repository counters")
    public static class CurrentCounters {

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

    /**
     * Right-rail content ordering to prioritize activity/releases above chat.
     */
    @Getter
    @Builder
    @Schema(description = "Right-rail module ordering")
    public static class RightRailOrdering {

        @Schema(description = "Activity module priority (1 = highest)")
        private int activityPriority;

        @Schema(description = "Releases module priority (2)")
        private int releasesPriority;

        @Schema(description = "Chat module priority (3 = below activity and releases)")
        private int chatPriority;

        @Schema(description = "Visible section IDs from dynamic section set")
        private List<String> visibleSectionIds;
    }
}
