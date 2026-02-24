package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.port.entity.Port;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.PortRepository;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.entity.WikiPublishedVersion;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import kr.devport.api.domain.wiki.repository.WikiPublishedVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PortRepository portRepository;

    @Mock
    private ProjectWikiSnapshotRepository wikiSnapshotRepository;

    @Mock
    private WikiPublishedVersionRepository wikiPublishedVersionRepository;

    @Mock
    private WikiSectionVisibilityService visibilityService;

    @InjectMocks
    private WikiService wikiService;

    @Test
    @DisplayName("project wiki response prioritizes latest published version for public reads")
    void getProjectWiki_usesLatestPublishedVersion() {
        Project project = Project.builder()
                .id(11L)
                .externalId("github:repo")
                .fullName("owner/repo")
                .build();

        WikiPublishedVersion latest = WikiPublishedVersion.builder()
                .projectId(11L)
                .versionNumber(3)
                .publishedAt(OffsetDateTime.now())
                .hiddenSections(List.of("chat"))
                .currentCounters(Map.of("stars", 10, "forks", 3, "watchers", 2, "openIssues", 1))
                .sections(List.of(
                        Map.of(
                                "sectionId", "what",
                                "heading", "What",
                                "anchor", "what",
                                "summary", "published summary",
                                "deepDiveMarkdown", "published deep"
                        )
                ))
                .build();

        when(projectRepository.findByExternalId("github:repo")).thenReturn(Optional.of(project));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(11L))
                .thenReturn(Optional.of(latest));
        when(visibilityService.getVisibleSectionData(eq(latest.getSectionsOrEmpty()), eq(latest.getHiddenSectionsOrEmpty())))
                .thenReturn(latest.getSectionsOrEmpty());

        WikiProjectPageResponse response = wikiService.getProjectWiki("github:repo");

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getAnchors()).hasSize(1);
        assertThat(response.getAnchors().get(0).getSectionId()).isEqualTo("what");
        assertThat(response.getSections().get(0).getSummary()).isEqualTo("published summary");
        assertThat(response.getCurrentCounters()).isNotNull();
        assertThat(response.getCurrentCounters().getStars()).isEqualTo(10);
        assertThat(response.getHiddenSections()).containsExactly("chat");
    }

    @Test
    @DisplayName("project wiki response falls back to legacy snapshot when no published version exists")
    void getProjectWiki_fallsBackToSnapshotForLegacyData() {
        Project project = Project.builder()
                .id(22L)
                .externalId("github:legacy")
                .fullName("owner/legacy")
                .build();

        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:legacy")
                .generatedAt(OffsetDateTime.now())
                .hiddenSections(List.of("chat"))
                .sections(List.of(
                        Map.of(
                                "sectionId", "what",
                                "heading", "What",
                                "anchor", "what",
                                "summary", "legacy summary",
                                "deepDiveMarkdown", "legacy deep"
                        )
                ))
                .isDataReady(true)
                .build();

        when(projectRepository.findByExternalId("github:legacy")).thenReturn(Optional.of(project));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(22L))
                .thenReturn(Optional.empty());
        when(wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady("github:legacy", true))
                .thenReturn(Optional.of(snapshot));
        when(visibilityService.getVisibleSectionData(eq(snapshot.getResolvedSections()), eq(snapshot.getHiddenSectionsOrEmpty())))
                .thenReturn(snapshot.getResolvedSections());

        WikiProjectPageResponse response = wikiService.getProjectWiki("github:legacy");

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getSections().get(0).getSummary()).isEqualTo("legacy summary");
    }

    @Test
    @DisplayName("domain browse summary uses latest published what section when available")
    void getProjectsByDomain_usesPublishedSummary() {
        Port port = Port.builder().slug("web").build();
        Project project = Project.builder()
                .id(1L)
                .externalId("github:repo")
                .fullName("owner/repo")
                .description("desc")
                .stars(123)
                .language("Java")
                .build();

        WikiPublishedVersion publishedVersion = WikiPublishedVersion.builder()
                .projectId(1L)
                .versionNumber(2)
                .publishedAt(OffsetDateTime.now())
                .sections(List.of(
                        Map.of(
                                "sectionId", "what",
                                "heading", "What",
                                "anchor", "what",
                                "summary", "wiki summary",
                                "deepDiveMarkdown", "wiki deep"
                        )
                ))
                .build();

        when(portRepository.findBySlug("web")).thenReturn(Optional.of(port));
        when(projectRepository.findByPort_Slug("web", Sort.by(Sort.Direction.DESC, "stars")))
                .thenReturn(List.of(project));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.of(publishedVersion));

        assertThat(wikiService.getProjectsByDomain("web").getProjects()).hasSize(1);
        assertThat(wikiService.getProjectsByDomain("web").getProjects().get(0).getSummary()).isEqualTo("wiki summary");
    }

    @Test
    @DisplayName("domain browse excludes projects with neither published versions nor ready snapshots")
    void getProjectsByDomain_excludesProjectsWithoutWikiSource() {
        Port port = Port.builder().slug("web").build();
        Project project = Project.builder()
                .id(9L)
                .externalId("github:none")
                .fullName("owner/none")
                .description("desc")
                .stars(1)
                .language("Java")
                .build();

        when(portRepository.findBySlug("web")).thenReturn(Optional.of(port));
        when(projectRepository.findByPort_Slug("web", Sort.by(Sort.Direction.DESC, "stars")))
                .thenReturn(List.of(project));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.empty());
        when(wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady("github:none", true))
                .thenReturn(Optional.empty());

        assertThat(wikiService.getProjectsByDomain("web").getProjects()).isEmpty();
    }
}
