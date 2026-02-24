package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiAuthoringWorkflowServiceTest {

    @Mock
    private WikiDraftRepository wikiDraftRepository;

    @InjectMocks
    private WikiAuthoringWorkflowService workflowService;

    @Test
    @DisplayName("createDraft persists API-owned summary/deep-dive section structure")
    void createDraft_persistsNormalizedSections() {
        when(wikiDraftRepository.save(any(WikiDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WikiDraft created = workflowService.createDraft(
                10L,
                List.of(Map.of(
                        "sectionId", "how",
                        "heading", "How",
                        "summary", "summary",
                        "deepDive", "deep"
                )),
                Map.of("stars", 100),
                List.of("chat"),
                null
        );

        assertThat(created.getProjectId()).isEqualTo(10L);
        assertThat(created.getSectionsOrEmpty()).hasSize(1);
        assertThat(created.getSectionsOrEmpty().get(0).get("deepDiveMarkdown")).isEqualTo("deep");
        assertThat(created.getCurrentCounters()).containsEntry("stars", 100);
        assertThat(created.getHiddenSectionsOrEmpty()).containsExactly("chat");
    }

    @Test
    @DisplayName("regenerateDraft replaces full section payload while preserving input order")
    void regenerateDraft_replacesSectionPayload() {
        WikiDraft existing = WikiDraft.builder()
                .projectId(33L)
                .sections(List.of(Map.of(
                        "sectionId", "what",
                        "summary", "old",
                        "deepDiveMarkdown", "old"
                )))
                .currentCounters(Map.of("stars", 1))
                .build();

        when(wikiDraftRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(wikiDraftRepository.save(any(WikiDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WikiDraft regenerated = workflowService.regenerateDraft(
                1L,
                List.of(
                        Map.of(
                                "sectionId", "what",
                                "summary", "new what",
                                "deepDiveMarkdown", "new deep"
                        ),
                        Map.of(
                                "sectionId", "architecture",
                                "summary", "arch",
                                "deepDiveMarkdown", "arch deep"
                        )
                ),
                Map.of("stars", 200),
                List.of("chat")
        );

        assertThat(regenerated.getSectionsOrEmpty()).hasSize(2);
        assertThat(regenerated.getSectionsOrEmpty().get(0).get("sectionId")).isEqualTo("what");
        assertThat(regenerated.getSectionsOrEmpty().get(1).get("sectionId")).isEqualTo("architecture");
        assertThat(regenerated.getCurrentCounters()).containsEntry("stars", 200);
    }

    @Test
    @DisplayName("editDraftSection applies last write wins for concurrent edits")
    void editDraftSection_lastWriteWins() {
        WikiDraft existing = WikiDraft.builder()
                .projectId(44L)
                .sections(List.of(Map.of(
                        "sectionId", "what",
                        "heading", "What",
                        "anchor", "what",
                        "summary", "initial",
                        "deepDiveMarkdown", "initial deep"
                )))
                .build();

        when(wikiDraftRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(wikiDraftRepository.save(any(WikiDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workflowService.editDraftSection(9L, "what", "writer A", "deep A");
        WikiDraft result = workflowService.editDraftSection(9L, "what", "writer B", "deep B");

        assertThat(result.getSectionsOrEmpty()).hasSize(1);
        assertThat(result.getSectionsOrEmpty().get(0).get("summary")).isEqualTo("writer B");
        assertThat(result.getSectionsOrEmpty().get(0).get("deepDiveMarkdown")).isEqualTo("deep B");
    }
}
