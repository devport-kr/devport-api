package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for project wiki page with Core-6 sections in summary-first progressive structure.
 * Architecture section explicitly includes generated diagram metadata when available.
 * Incomplete sections are omitted by visibility service.
 */
@Getter
@Builder
@Schema(description = "Project wiki page with Core-6 sections and right-rail content ordering")
public class WikiProjectPageResponse {

    @Schema(description = "Project external ID", example = "github:12345")
    private String projectExternalId;

    @Schema(description = "Repository full name", example = "vercel/next.js")
    private String fullName;

    @Schema(description = "Snapshot generation timestamp")
    private OffsetDateTime generatedAt;

    // Core-6 sections (only visible sections included)
    @Schema(description = "What this project is - purpose, domain, users")
    private WikiSection what;

    @Schema(description = "How it works - key concepts, workflows, usage")
    private WikiSection how;

    @Schema(description = "Architecture and codebase - structure, components, design, generated diagrams")
    private ArchitectureSection architecture;

    @Schema(description = "Repository activity - 12-month event history")
    private WikiSection activity;

    @Schema(description = "Releases and tags - timeline with narrative")
    private WikiSection releases;

    // Right-rail ordering payload
    @Schema(description = "Right-rail modules in priority order: activity, releases, chat")
    private RightRailOrdering rightRail;

    /**
     * Standard wiki section with progressive disclosure (summary + deep dive).
     */
    @Getter
    @Builder
    @Schema(description = "Wiki section with progressive disclosure")
    public static class WikiSection {

        @Schema(description = "Short summary paragraph (1-3 sentences)")
        private String summary;

        @Schema(description = "Full technical explanation in markdown format")
        private String deepDiveMarkdown;

        @Schema(description = "Whether this section should be expanded by default")
        private boolean defaultExpanded;
    }

    /**
     * Architecture section extends WikiSection with generated diagram support.
     * Diagram blocks are explicitly included when available and omitted when absent.
     */
    @Getter
    @Builder
    @Schema(description = "Architecture section with optional generated diagrams")
    public static class ArchitectureSection {

        @Schema(description = "Architecture summary (1-3 sentences)")
        private String summary;

        @Schema(description = "Deep technical architecture explanation in markdown")
        private String deepDiveMarkdown;

        @Schema(description = "Whether this section should be expanded by default")
        private boolean defaultExpanded;

        @Schema(description = "Generated diagram DSL (Mermaid) if available, null otherwise")
        private String generatedDiagramDsl;

        @Schema(description = "Diagram metadata and render hints if diagram is available")
        private DiagramMetadata diagramMetadata;
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

        @Schema(description = "Visible section IDs for anchor navigation stability")
        private List<String> visibleSections;
    }
}
