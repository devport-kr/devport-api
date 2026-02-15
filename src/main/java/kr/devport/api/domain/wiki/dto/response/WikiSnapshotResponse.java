package kr.devport.api.domain.wiki.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Wiki snapshot response DTO aligned to crawler WikiSnapshot contract.
 * Core-6 sections with progressive disclosure, readiness metadata, and section hiding controls.
 */
@Getter
@Builder
@Schema(description = "Project wiki snapshot with Core-6 sections and readiness metadata")
public class WikiSnapshotResponse {

    @Schema(description = "Project external ID", example = "github:12345")
    private String projectExternalId;

    @Schema(description = "Snapshot generation timestamp")
    private OffsetDateTime generatedAt;

    // Core-6 sections
    @Schema(description = "What this project is - purpose, domain, users")
    private WikiSectionResponse what;

    @Schema(description = "How it works - key concepts, workflows, usage")
    private WikiSectionResponse how;

    @Schema(description = "Architecture and codebase - structure, components, design")
    private WikiSectionResponse architecture;

    @Schema(description = "Repository activity - 12-month event history")
    private WikiSectionResponse activity;

    @Schema(description = "Releases and tags - timeline with narrative")
    private WikiSectionResponse releases;

    @Schema(description = "Chat module payload - repo context for Q&A")
    private WikiSectionResponse chat;

    // Readiness and hiding controls
    @Schema(description = "Whether this project meets minimum data quality thresholds")
    private boolean isDataReady;

    @Schema(description = "Section names to hide due to incomplete data")
    private List<String> hiddenSections;

    @Schema(description = "Detailed readiness scoring and gate results")
    private Map<String, Object> readinessMetadata;

    /**
     * Individual wiki section with summary + deep dive content.
     */
    @Getter
    @Builder
    @Schema(description = "Wiki section with progressive disclosure")
    public static class WikiSectionResponse {

        @Schema(description = "Short summary paragraph (1-3 sentences)", example = "Next.js is a React framework for production-grade web applications.")
        private String summary;

        @Schema(description = "Full technical explanation in markdown format")
        private String deepDiveMarkdown;

        @Schema(description = "Whether this section should be expanded by default")
        private boolean defaultExpanded;

        @Schema(description = "Optional Mermaid DSL for generated diagrams")
        private String generatedDiagramDsl;
    }
}
