package kr.devport.api.domain.wiki.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectWikiSnapshotTest {

    @Test
    @DisplayName("resolved sections returns dynamic sections payload")
    void getResolvedSections_returnsDynamicSections() {
        List<Map<String, Object>> sections = List.of(
                Map.of(
                        "sectionId", "what",
                        "heading", "What",
                        "anchor", "what",
                        "summary", "Summary",
                        "deepDiveMarkdown", "Deep dive"
                )
        );

        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .sections(sections)
                .isDataReady(true)
                .build();

        assertThat(snapshot.getResolvedSections()).hasSize(1);
        assertThat(snapshot.getResolvedSections().get(0).get("sectionId")).isEqualTo("what");
    }

    @Test
    @DisplayName("resolved sections returns empty list when null")
    void getResolvedSections_returnsEmptyWhenNull() {
        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .isDataReady(true)
                .build();

        assertThat(snapshot.getResolvedSections()).isEmpty();
    }

    @Test
    @DisplayName("resolved current counters returns counters directly")
    void getResolvedCurrentCounters_returnsCounters() {
        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .currentCounters(Map.of("stars", 10, "forks", 3))
                .isDataReady(true)
                .build();

        assertThat(snapshot.getResolvedCurrentCounters())
                .containsEntry("stars", 10)
                .containsEntry("forks", 3);
    }

    @Test
    @DisplayName("resolved current counters returns empty map when null")
    void getResolvedCurrentCounters_returnsEmptyWhenNull() {
        ProjectWikiSnapshot snapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .isDataReady(true)
                .build();

        assertThat(snapshot.getResolvedCurrentCounters()).isEmpty();
    }
}
