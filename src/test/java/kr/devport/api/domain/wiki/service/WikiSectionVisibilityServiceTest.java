package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikiSectionVisibilityServiceTest {

    private final WikiSectionVisibilityService visibilityService = new WikiSectionVisibilityService();

    @Test
    @DisplayName("visible section list excludes hidden, incomplete, and uncertain sections")
    void getVisibleSectionData_filtersByReadinessAndHiddenStatus() {
        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .sections(List.of(
                        Map.of(
                                "sectionId", "what",
                                "heading", "What",
                                "anchor", "what",
                                "summary", "ready",
                                "deepDiveMarkdown", "ready deep"
                        ),
                        Map.of(
                                "sectionId", "how",
                                "heading", "How",
                                "anchor", "how",
                                "summary", "",
                                "deepDiveMarkdown", "missing summary"
                        ),
                        Map.of(
                                "sectionId", "architecture",
                                "heading", "Architecture",
                                "anchor", "architecture",
                                "summary", "uncertain",
                                "deepDiveMarkdown", "details",
                                "metadata", Map.of("uncertain", true)
                        )
                ))
                .hiddenSections(List.of("chat"))
                .isDataReady(true)
                .build();

        assertThat(visibilityService.getVisibleSectionData(snapshot)).hasSize(1);
        assertThat(visibilityService.getVisibleSections(snapshot)).containsExactly("what");
    }
}
