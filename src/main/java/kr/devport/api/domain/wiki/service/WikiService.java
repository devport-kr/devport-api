package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.port.entity.Port;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.PortRepository;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiDomainBrowseResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wiki read orchestration and response mapping service.
 * Coordinates between Project/Port repositories and wiki snapshot data,
 * enforces readiness-gated browse filtering, and maps Core-6 sections to response DTOs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiService {

    private final ProjectRepository projectRepository;
    private final PortRepository portRepository;
    private final ProjectWikiSnapshotRepository wikiSnapshotRepository;
    private final WikiSectionVisibilityService visibilityService;

    /**
     * Browse wiki-eligible projects by domain (port slug).
     * Returns top-starred data-ready projects only.
     *
     * @param domain Port slug (e.g., "web-frameworks")
     * @return WikiDomainBrowseResponse with project summaries
     */
    public WikiDomainBrowseResponse getProjectsByDomain(String domain) {
        // Fetch port to validate domain exists
        Port port = portRepository.findBySlug(domain)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domain));

        // Fetch projects in this port, ordered by stars descending
        List<Project> projects = projectRepository.findByPort_Slug(domain, Sort.by(Sort.Direction.DESC, "stars"));

        // Filter to data-ready projects only and map to summaries
        List<WikiDomainBrowseResponse.ProjectSummary> projectSummaries = projects.stream()
                .filter(project -> {
                    // Only include projects with data-ready wiki snapshots
                    Optional<ProjectWikiSnapshot> snapshot = 
                            wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady(
                                    project.getExternalId(), true);
                    return snapshot.isPresent();
                })
                .map(project -> {
                    // Fetch snapshot for summary field
                    ProjectWikiSnapshot snapshot = wikiSnapshotRepository
                            .findByProjectExternalIdAndIsDataReady(project.getExternalId(), true)
                            .orElseThrow();
                    
                    Map<String, Object> whatSection = snapshot.getWhatSection();
                    String summary = whatSection != null ? 
                            (String) whatSection.getOrDefault("summary", "") : "";

                    return WikiDomainBrowseResponse.ProjectSummary.builder()
                            .projectExternalId(project.getExternalId())
                            .fullName(project.getFullName())
                            .description(project.getDescription())
                            .stars(project.getStars())
                            .language(project.getLanguage())
                            .summary(summary)
                            .build();
                })
                .collect(Collectors.toList());

        return WikiDomainBrowseResponse.builder()
                .domain(domain)
                .projects(projectSummaries)
                .build();
    }

    /**
     * Get project wiki page with Core-6 sections in summary-first progressive structure.
     * Enforces hide-incomplete section policy via visibility service.
     * Explicitly maps generated architecture diagrams when available.
     *
     * @param projectExternalId Project external ID (e.g., "github:12345")
     * @return WikiProjectPageResponse with visible sections only
     */
    public WikiProjectPageResponse getProjectWiki(String projectExternalId) {
        // Fetch project metadata
        Project project = projectRepository.findByExternalId(projectExternalId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectExternalId));

        // Fetch data-ready wiki snapshot
        ProjectWikiSnapshot snapshot = wikiSnapshotRepository
                .findByProjectExternalIdAndIsDataReady(projectExternalId, true)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wiki snapshot not ready for project: " + projectExternalId));

        // Get visible sections from visibility service
        List<String> visibleSections = visibilityService.getVisibleSections(snapshot);

        // Build response with only visible sections
        WikiProjectPageResponse.WikiProjectPageResponseBuilder builder = WikiProjectPageResponse.builder()
                .projectExternalId(projectExternalId)
                .fullName(project.getFullName())
                .generatedAt(snapshot.getGeneratedAt());

        // Map Core-6 sections conditionally based on visibility
        if (visibilityService.isSectionVisible(snapshot, "what")) {
            builder.what(mapWikiSection(snapshot.getWhatSection()));
        }

        if (visibilityService.isSectionVisible(snapshot, "how")) {
            builder.how(mapWikiSection(snapshot.getHowSection()));
        }

        if (visibilityService.isSectionVisible(snapshot, "architecture")) {
            builder.architecture(mapArchitectureSection(snapshot.getArchitectureSection()));
        }

        if (visibilityService.isSectionVisible(snapshot, "activity")) {
            builder.activity(mapWikiSection(snapshot.getActivitySection()));
        }

        if (visibilityService.isSectionVisible(snapshot, "releases")) {
            builder.releases(mapWikiSection(snapshot.getReleasesSection()));
        }

        // Right-rail ordering: activity(1), releases(2), chat(3)
        WikiProjectPageResponse.RightRailOrdering rightRail = 
                WikiProjectPageResponse.RightRailOrdering.builder()
                        .activityPriority(1)
                        .releasesPriority(2)
                        .chatPriority(3)
                        .visibleSections(visibleSections)
                        .build();

        builder.rightRail(rightRail);

        return builder.build();
    }

    /**
     * Map standard wiki section (what, how, activity, releases) from JSON to DTO.
     */
    private WikiProjectPageResponse.WikiSection mapWikiSection(Map<String, Object> sectionData) {
        if (sectionData == null || sectionData.isEmpty()) {
            return null;
        }

        return WikiProjectPageResponse.WikiSection.builder()
                .summary((String) sectionData.getOrDefault("summary", ""))
                .deepDiveMarkdown((String) sectionData.getOrDefault("deepDiveMarkdown", ""))
                .defaultExpanded((Boolean) sectionData.getOrDefault("defaultExpanded", false))
                .build();
    }

    /**
     * Map architecture section with generated diagram support.
     * Explicitly includes diagram DSL and metadata when available,
     * omits diagram blocks when data is absent.
     */
    private WikiProjectPageResponse.ArchitectureSection mapArchitectureSection(Map<String, Object> sectionData) {
        if (sectionData == null || sectionData.isEmpty()) {
            return null;
        }

        WikiProjectPageResponse.ArchitectureSection.ArchitectureSectionBuilder builder =
                WikiProjectPageResponse.ArchitectureSection.builder()
                        .summary((String) sectionData.getOrDefault("summary", ""))
                        .deepDiveMarkdown((String) sectionData.getOrDefault("deepDiveMarkdown", ""))
                        .defaultExpanded((Boolean) sectionData.getOrDefault("defaultExpanded", false));

        // Include diagram fields only if present
        String diagramDsl = (String) sectionData.get("generatedDiagramDsl");
        if (diagramDsl != null && !diagramDsl.isBlank()) {
            builder.generatedDiagramDsl(diagramDsl);

            // Map diagram metadata if available
            @SuppressWarnings("unchecked")
            Map<String, Object> diagramMetadataMap = (Map<String, Object>) sectionData.get("diagramMetadata");
            if (diagramMetadataMap != null) {
                WikiProjectPageResponse.DiagramMetadata diagramMetadata =
                        WikiProjectPageResponse.DiagramMetadata.builder()
                                .diagramType((String) diagramMetadataMap.getOrDefault("diagramType", ""))
                                .altText((String) diagramMetadataMap.getOrDefault("altText", ""))
                                .renderHints((String) diagramMetadataMap.getOrDefault("renderHints", ""))
                                .build();
                builder.diagramMetadata(diagramMetadata);
            }
        }

        return builder.build();
    }
}
