package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.common.cache.CacheNames;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wiki read orchestration service. Serves wiki pages directly from wiki_section_chunks.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiService {

    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("\\d+$");

    private final ProjectRepository projectRepository;
    private final WikiSectionChunkRepository wikiSectionChunkRepository;

    @Cacheable(cacheNames = CacheNames.WIKI_PROJECTS)
    public WikiProjectListResponse getProjects() {
        List<Project> projects = projectRepository.findAll(Sort.by(Sort.Direction.DESC, "stars"));

        // Single query to fetch all summary chunks across all projects (avoids N+1)
        Map<String, String> summaryByProject = wikiSectionChunkRepository.findAllSummaryChunks().stream()
                .collect(Collectors.toMap(
                        WikiSectionChunk::getProjectExternalId,
                        WikiSectionChunk::getContent,
                        (existing, replacement) -> existing  // keep first if duplicates
                ));

        List<WikiProjectListResponse.ProjectSummary> projectSummaries = projects.stream()
                .map(project -> {
                    String summary = summaryByProject.getOrDefault(project.getExternalId(), "");
                    if (summary.isBlank()) {
                        return null;
                    }
                    return WikiProjectListResponse.ProjectSummary.builder()
                            .projectExternalId(project.getExternalId())
                            .fullName(project.getFullName())
                            .description(project.getDescription())
                            .stars(project.getStars())
                            .language(project.getLanguage())
                            .summary(summary)
                            .build();
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());

        return WikiProjectListResponse.builder()
                .projects(projectSummaries)
                .build();
    }

    @Cacheable(cacheNames = CacheNames.WIKI_PROJECT_PAGE, key = "#projectExternalId")
    public WikiProjectPageResponse getProjectWiki(String projectExternalId) {
        Project project = projectRepository.findByExternalId(projectExternalId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectExternalId));

        List<WikiSectionChunk> chunks = wikiSectionChunkRepository.findByProjectExternalId(projectExternalId);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No wiki content found for project: " + projectExternalId);
        }

        Map<String, List<WikiSectionChunk>> bySectionId = chunks.stream()
                .collect(Collectors.groupingBy(WikiSectionChunk::getSectionId));

        List<String> sortedSectionIds = bySectionId.keySet().stream()
                .sorted(Comparator.comparingInt(this::extractTrailingNumber))
                .collect(Collectors.toList());

        OffsetDateTime generatedAt = chunks.stream()
                .map(WikiSectionChunk::getUpdatedAt)
                .filter(t -> t != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<WikiProjectPageResponse.WikiSection> sections = sortedSectionIds.stream()
                .map(sectionId -> buildSection(sectionId, bySectionId.get(sectionId)))
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

        WikiProjectPageResponse.CurrentCounters currentCounters = WikiProjectPageResponse.CurrentCounters.builder()
                .stars(project.getStars())
                .forks(project.getForks())
                .build();

        return WikiProjectPageResponse.builder()
                .projectExternalId(projectExternalId)
                .fullName(project.getFullName())
                .generatedAt(generatedAt)
                .sections(sections)
                .anchors(anchors)
                .currentCounters(currentCounters)
                .rightRail(rightRail)
                .build();
    }

    private WikiProjectPageResponse.WikiSection buildSection(String sectionId, List<WikiSectionChunk> sectionChunks) {
        WikiSectionChunk summaryChunk = sectionChunks.stream()
                .filter(c -> "summary".equals(c.getChunkType()) && c.getSubsectionId() == null)
                .findFirst()
                .orElse(null);

        String summary = summaryChunk != null ? summaryChunk.getContent() : "";
        String heading = sectionId;
        if (summaryChunk != null && summaryChunk.getMetadata() != null) {
            Object titleKo = summaryChunk.getMetadata().get("titleKo");
            if (titleKo != null && !String.valueOf(titleKo).isBlank()) {
                heading = String.valueOf(titleKo);
            }
        }

        if (heading.equals(sectionId)) {
            WikiSectionChunk overviewChunk = sectionChunks.stream()
                    .filter(c -> "overview".equals(c.getChunkType()))
                    .findFirst()
                    .orElse(null);
            if (overviewChunk != null && overviewChunk.getMetadata() != null) {
                Object titleKo = overviewChunk.getMetadata().get("titleKo");
                if (titleKo != null && !String.valueOf(titleKo).isBlank()) {
                    heading = String.valueOf(titleKo);
                }
            }
        }

        String deepDiveMarkdown = sectionChunks.stream()
                .filter(c -> "body".equals(c.getChunkType()) || "overview".equals(c.getChunkType()))
                .sorted(Comparator.comparingInt(c -> extractTrailingNumber(c.getSubsectionId())))
                .map(c -> {
                    if ("overview".equals(c.getChunkType())) {
                        return c.getContent();
                    }
                    String titleKo = null;
                    if (c.getMetadata() != null) {
                        Object t = c.getMetadata().get("titleKo");
                        if (t != null && !String.valueOf(t).isBlank()) {
                            titleKo = String.valueOf(t);
                        }
                    }
                    return titleKo != null ? "## " + titleKo + "\n\n" + c.getContent() : c.getContent();
                })
                .collect(Collectors.joining("\n\n"));

        return WikiProjectPageResponse.WikiSection.builder()
                .sectionId(sectionId)
                .heading(heading)
                .anchor(sectionId)
                .summary(summary)
                .deepDiveMarkdown(deepDiveMarkdown)
                .build();
    }

    private int extractTrailingNumber(String id) {
        if (id == null) {
            return Integer.MAX_VALUE;
        }
        Matcher m = NUMERIC_SUFFIX.matcher(id);
        return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
    }
}
