package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import kr.devport.api.domain.common.cache.CacheScope;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiFreshnessSignalServiceTest {

    @Mock
    private ProjectWikiSnapshotRepository projectWikiSnapshotRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WikiDraftRepository wikiDraftRepository;

    @Mock
    private WikiAuthoringWorkflowService wikiAuthoringWorkflowService;

    @InjectMocks
    private WikiFreshnessSignalService wikiFreshnessSignalService;

    @Test
    @DisplayName("handleCrawlerCompletion stores freshness signals and triggers API-owned draft regeneration")
    void handleCrawlerCompletion_storesFreshnessAndRegeneratesDraft() {
        CrawlerJobCompletedRequest request = CrawlerJobCompletedRequest.builder()
                .jobId("job-1")
                .scope(CacheScope.GIT_REPO)
                .completedAt("2026-02-16T00:10:00Z")
                .signature("sig")
                .freshnessSignals(List.of(Map.of(
                        "projectExternalId", "github:owner/repo",
                        "generatedAt", "2026-02-16T00:00:00Z",
                        "currentCounters", Map.of("stars", 200, "forks", 30),
                        "readinessMetadata", Map.of("passesTopStarGate", true),
                        "hiddenSectionIds", List.of("chat")
                )))
                .build();

        Project project = Project.builder().id(7L).externalId("github:owner/repo").build();
        WikiDraft draft = mock(WikiDraft.class);
        when(draft.getId()).thenReturn(99L);
        when(draft.getSectionsOrEmpty()).thenReturn(List.of(Map.of(
                "sectionId", "what",
                "summary", "summary",
                "deepDiveMarkdown", "deep"
        )));

        when(projectWikiSnapshotRepository.findByProjectExternalId("github:owner/repo"))
                .thenReturn(Optional.empty());
        when(projectRepository.findByExternalId("github:owner/repo")).thenReturn(Optional.of(project));
        when(wikiDraftRepository.findTopByProjectIdOrderByUpdatedAtDesc(7L)).thenReturn(Optional.of(draft));
        when(projectWikiSnapshotRepository.save(any(ProjectWikiSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        wikiFreshnessSignalService.handleCrawlerCompletion(request);

        ArgumentCaptor<ProjectWikiSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ProjectWikiSnapshot.class);
        verify(projectWikiSnapshotRepository).save(snapshotCaptor.capture());
        ProjectWikiSnapshot saved = snapshotCaptor.getValue();

        assertThat(saved.getProjectExternalId()).isEqualTo("github:owner/repo");
        assertThat(saved.getResolvedSections()).isEmpty();
        assertThat(saved.getResolvedCurrentCounters()).containsEntry("stars", 200);
        assertThat(saved.getHiddenSectionsOrEmpty()).containsExactly("chat");

        verify(wikiAuthoringWorkflowService).regenerateDraft(
                7L,
                99L,
                draft.getSectionsOrEmpty(),
                Map.of("stars", 200, "forks", 30),
                List.of("chat")
        );
    }

    @Test
    @DisplayName("handleCrawlerCompletion ignores crawler-owned narrative payload fields")
    void handleCrawlerCompletion_ignoresNarrativePayload() {
        CrawlerJobCompletedRequest request = CrawlerJobCompletedRequest.builder()
                .jobId("job-2")
                .scope(CacheScope.GIT_REPO)
                .completedAt("2026-02-16T00:10:00Z")
                .signature("sig")
                .freshnessSignals(List.of(Map.of(
                        "projectExternalId", "github:owner/repo",
                        "sections", List.of(Map.of("sectionId", "what", "summary", "crawler text")),
                        "currentCounters", Map.of("stars", 200)
                )))
                .build();

        wikiFreshnessSignalService.handleCrawlerCompletion(request);

        verify(projectWikiSnapshotRepository, never()).save(any(ProjectWikiSnapshot.class));
        verify(wikiAuthoringWorkflowService, never()).regenerateDraft(any(), any(), any(), any(), any());
    }
}
