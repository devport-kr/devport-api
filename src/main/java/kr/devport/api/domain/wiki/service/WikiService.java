package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.port.entity.Port;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.PortRepository;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiDomainBrowseResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.entity.WikiPublishedVersion;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import kr.devport.api.domain.wiki.repository.WikiPublishedVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wiki read orchestration and response mapping service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiService {

    private final ProjectRepository projectRepository;
    private final PortRepository portRepository;
    private final ProjectWikiSnapshotRepository wikiSnapshotRepository;
    private final WikiPublishedVersionRepository wikiPublishedVersionRepository;
    private final WikiSectionVisibilityService visibilityService;

    public WikiDomainBrowseResponse getProjectsByDomain(String domain) {
        Port port = portRepository.findBySlug(domain)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domain));

        List<Project> projects = projectRepository.findByPort_Slug(domain, Sort.by(Sort.Direction.DESC, "stars"));

        List<WikiDomainBrowseResponse.ProjectSummary> projectSummaries = projects.stream()
                .map(project -> {
                    String summary = resolveBrowseSummary(project);
                    if (summary.isBlank()) {
                        return null;
                    }

                    return WikiDomainBrowseResponse.ProjectSummary.builder()
                            .projectExternalId(project.getExternalId())
                            .fullName(project.getFullName())
                            .description(project.getDescription())
                            .stars(project.getStars())
                            .language(project.getLanguage())
                            .summary(summary)
                            .build();
                })
                .filter(summary -> summary != null)
                .collect(Collectors.toList());

        return WikiDomainBrowseResponse.builder()
                .domain(port.getSlug())
                .projects(projectSummaries)
                .build();
    }

    public WikiProjectPageResponse getProjectWiki(String projectExternalId) {
        Project project = projectRepository.findByExternalId(projectExternalId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectExternalId));

        WikiContentSource contentSource = resolvePublicContent(project);

        List<Map<String, Object>> visibleSectionData = visibilityService.getVisibleSectionData(
                contentSource.sections(),
                contentSource.hiddenSections()
        );
        List<WikiProjectPageResponse.WikiSection> sections = visibleSectionData.stream()
                .map(this::mapWikiSection)
                .collect(Collectors.toList());

        List<WikiProjectPageResponse.AnchorItem> anchors = sections.stream()
                .map(section -> WikiProjectPageResponse.AnchorItem.builder()
                        .sectionId(section.getSectionId())
                        .heading(section.getHeading())
                        .anchor(section.getAnchor())
                        .build())
                .collect(Collectors.toList());

        WikiProjectPageResponse.RightRailOrdering rightRail = WikiProjectPageResponse.RightRailOrdering.builder()
                .activityPriority(1)
                .releasesPriority(2)
                .chatPriority(3)
                .visibleSectionIds(anchors.stream().map(WikiProjectPageResponse.AnchorItem::getSectionId).toList())
                .build();

        return WikiProjectPageResponse.builder()
                .projectExternalId(projectExternalId)
                .fullName(project.getFullName())
                .generatedAt(contentSource.generatedAt())
                .sections(sections)
                .anchors(anchors)
                .hiddenSections(contentSource.hiddenSections())
                .currentCounters(mapCurrentCounters(contentSource.currentCounters()))
                .rightRail(rightRail)
                .build();
    }

    private String resolveBrowseSummary(Project project) {
        Optional<WikiPublishedVersion> published = wikiPublishedVersionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(project.getId());
        if (published.isPresent()) {
            return extractBrowseSummary(published.get().getSectionsOrEmpty());
        }

        return wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady(project.getExternalId(), true)
                .map(snapshot -> extractBrowseSummary(snapshot.getResolvedSections()))
                .orElse("");
    }

    private WikiContentSource resolvePublicContent(Project project) {
        Optional<WikiPublishedVersion> published = wikiPublishedVersionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(project.getId());
        if (published.isPresent()) {
            WikiPublishedVersion latest = published.get();
            return new WikiContentSource(
                    latest.getSectionsOrEmpty(),
                    latest.getHiddenSectionsOrEmpty(),
                    latest.getCurrentCounters(),
                    latest.getPublishedAt()
            );
        }

        ProjectWikiSnapshot snapshot = wikiSnapshotRepository
                .findByProjectExternalIdAndIsDataReady(project.getExternalId(), true)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wiki snapshot not ready for project: " + project.getExternalId()));

        return new WikiContentSource(
                snapshot.getResolvedSections(),
                snapshot.getHiddenSectionsOrEmpty(),
                snapshot.getResolvedCurrentCounters(),
                snapshot.getGeneratedAt()
        );
    }

    private String extractBrowseSummary(List<Map<String, Object>> sectionPayload) {
        Optional<Map<String, Object>> whatSection = sectionPayload.stream()
                .filter(section -> "what".equals(String.valueOf(section.get("sectionId"))))
                .findFirst();

        if (whatSection.isPresent()) {
            return String.valueOf(whatSection.get().getOrDefault("summary", ""));
        }

        return sectionPayload.stream()
                .map(section -> String.valueOf(section.getOrDefault("summary", "")))
                .filter(summary -> !summary.isBlank())
                .findFirst()
                .orElse("");
    }

    private record WikiContentSource(
            List<Map<String, Object>> sections,
            List<String> hiddenSections,
            Map<String, Object> currentCounters,
            OffsetDateTime generatedAt
    ) {
    }

    private WikiProjectPageResponse.WikiSection mapWikiSection(Map<String, Object> sectionData) {
        String sectionId = asText(sectionData.get("sectionId"));
        String heading = asText(sectionData.get("heading"));
        String anchor = asText(sectionData.get("anchor"));
        String summary = asText(sectionData.get("summary"));
        String deepDiveMarkdown = asText(sectionData.getOrDefault("deepDiveMarkdown", sectionData.get("deepDive")));

        WikiProjectPageResponse.WikiSection.WikiSectionBuilder builder = WikiProjectPageResponse.WikiSection.builder()
                .sectionId(sectionId)
                .heading(heading.isBlank() ? sectionId : heading)
                .anchor(anchor.isBlank() ? sectionId : anchor)
                .summary(summary)
                .deepDiveMarkdown(deepDiveMarkdown)
                .defaultExpanded(Boolean.TRUE.equals(sectionData.get("defaultExpanded")))
                .generatedDiagramDsl(asText(sectionData.get("generatedDiagramDsl")));

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) sectionData.get("metadata");
        if (metadata != null && !metadata.isEmpty()) {
            builder.metadata(metadata);

            @SuppressWarnings("unchecked")
            Map<String, Object> diagramMetadataMap = (Map<String, Object>) metadata.get("diagramMetadata");
            if (diagramMetadataMap != null && !diagramMetadataMap.isEmpty()) {
                builder.diagramMetadata(WikiProjectPageResponse.DiagramMetadata.builder()
                        .diagramType(asText(diagramMetadataMap.get("diagramType")))
                        .altText(asText(diagramMetadataMap.get("altText")))
                        .renderHints(asText(diagramMetadataMap.get("renderHints")))
                        .build());
            }
        }

        return builder.build();
    }

    private WikiProjectPageResponse.CurrentCounters mapCurrentCounters(Map<String, Object> counters) {
        if (counters == null || counters.isEmpty()) {
            return null;
        }

        return WikiProjectPageResponse.CurrentCounters.builder()
                .stars(asInteger(counters.get("stars")))
                .forks(asInteger(counters.get("forks")))
                .watchers(asInteger(counters.get("watchers")))
                .openIssues(asInteger(counters.get("openIssues")))
                .updatedAt(asOffsetDateTime(counters.get("updatedAt")))
                .build();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private OffsetDateTime asOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof OffsetDateTime time) {
            return time;
        }

        try {
            return OffsetDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
