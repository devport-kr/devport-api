package kr.devport.api.domain.wiki.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikiDraftTest {

    @Test
    @DisplayName("draft holds project-scoped sections and editable payload")
    void draftSupportsProjectScopedPayload() {
        WikiDraft draft = WikiDraft.builder()
                .projectId(10L)
                .sections(List.of(Map.of(
                        "sectionId", "what",
                        "summary", "old",
                        "deepDiveMarkdown", "old deep"
                )))
                .currentCounters(Map.of("stars", 12))
                .hiddenSections(List.of("chat"))
                .sourcePublishedVersionId(2L)
                .build();

        draft.replaceSections(List.of(Map.of(
                "sectionId", "what",
                "summary", "new",
                "deepDiveMarkdown", "new deep"
        )));

        assertThat(draft.getProjectId()).isEqualTo(10L);
        assertThat(draft.getSourcePublishedVersionId()).isEqualTo(2L);
        assertThat(draft.getSectionsOrEmpty()).hasSize(1);
        assertThat(draft.getSectionsOrEmpty().get(0).get("summary")).isEqualTo("new");
        assertThat(draft.getCurrentCounters()).containsEntry("stars", 12);
        assertThat(draft.getHiddenSectionsOrEmpty()).containsExactly("chat");
    }
}
