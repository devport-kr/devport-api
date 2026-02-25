package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiProjectListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import kr.devport.api.domain.wiki.repository.WikiSectionChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WikiSectionChunkRepository wikiSectionChunkRepository;

    @InjectMocks
    private WikiService wikiService;

    @Test
    @DisplayName("getProjectWiki builds sections from summary and body chunks")
    void getProjectWiki_buildsSectionsFromChunks() {
        Project project = Project.builder()
                .id(11L)
                .externalId("github:repo")
                .fullName("owner/repo")
                .stars(100)
                .forks(10)
                .build();

        WikiSectionChunk summaryChunk = WikiSectionChunk.builder()
                .projectExternalId("github:repo")
                .sectionId("sec-1")
                .subsectionId(null)
                .chunkType("summary")
                .content("section one summary")
                .metadata(Map.of("titleKo", "섹션 하나"))
                .commitSha("abc")
                .build();

        WikiSectionChunk bodyChunk = WikiSectionChunk.builder()
                .projectExternalId("github:repo")
                .sectionId("sec-1")
                .subsectionId("sub-1-1")
                .chunkType("body")
                .content("body content one")
                .metadata(Map.of("titleKo", "섹션 하나 — 세부"))
                .commitSha("abc")
                .build();

        when(projectRepository.findByExternalId("github:repo")).thenReturn(Optional.of(project));
        when(wikiSectionChunkRepository.findByProjectExternalId("github:repo"))
                .thenReturn(List.of(summaryChunk, bodyChunk));

        WikiProjectPageResponse response = wikiService.getProjectWiki("github:repo");

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getSections().get(0).getSectionId()).isEqualTo("sec-1");
        assertThat(response.getSections().get(0).getHeading()).isEqualTo("섹션 하나");
        assertThat(response.getSections().get(0).getSummary()).isEqualTo("section one summary");
        assertThat(response.getSections().get(0).getDeepDiveMarkdown()).isEqualTo("body content one");
        assertThat(response.getAnchors()).hasSize(1);
        assertThat(response.getCurrentCounters()).isNotNull();
        assertThat(response.getCurrentCounters().getStars()).isEqualTo(100);
    }

    @Test
    @DisplayName("getProjects uses first summary chunk for browse summary")
    void getProjects_usesSummaryChunk() {
        Project project = Project.builder()
                .id(1L)
                .externalId("github:repo")
                .fullName("owner/repo")
                .description("desc")
                .stars(123)
                .language("Java")
                .build();

        WikiSectionChunk summaryChunk = WikiSectionChunk.builder()
                .projectExternalId("github:repo")
                .sectionId("sec-1")
                .subsectionId(null)
                .chunkType("summary")
                .content("browse summary text")
                .commitSha("abc")
                .build();

        when(projectRepository.findAll(any(Sort.class))).thenReturn(List.of(project));
        when(wikiSectionChunkRepository.findByProjectExternalId("github:repo"))
                .thenReturn(List.of(summaryChunk));

        WikiProjectListResponse response = wikiService.getProjects();

        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getProjects().get(0).getSummary()).isEqualTo("browse summary text");
    }

    @Test
    @DisplayName("getProjects excludes projects with no chunks")
    void getProjects_excludesProjectsWithNoChunks() {
        Project project = Project.builder()
                .id(9L)
                .externalId("github:none")
                .fullName("owner/none")
                .description("desc")
                .stars(1)
                .language("Java")
                .build();

        when(projectRepository.findAll(any(Sort.class))).thenReturn(List.of(project));
        when(wikiSectionChunkRepository.findByProjectExternalId("github:none"))
                .thenReturn(List.of());

        assertThat(wikiService.getProjects().getProjects()).isEmpty();
    }
}
