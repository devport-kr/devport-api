package kr.devport.api.domain.wiki.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikiSnapshotResponseTest {

    @Test
    @DisplayName("snapshot response exposes dynamic sections and current counters")
    void builder_setsDynamicSectionContract() {
        WikiSnapshotResponse response = WikiSnapshotResponse.builder()
                .projectExternalId("github:repo")
                .generatedAt(OffsetDateTime.now())
                .sections(List.of(
                        WikiSnapshotResponse.WikiSectionResponse.builder()
                                .sectionId("what")
                                .heading("What")
                                .anchor("what")
                                .summary("Summary")
                                .deepDiveMarkdown("Deep")
                                .metadata(Map.of("category", "core"))
                                .build()
                ))
                .currentCounters(WikiSnapshotResponse.CurrentCountersResponse.builder()
                        .stars(100)
                        .forks(40)
                        .watchers(12)
                        .openIssues(5)
                        .updatedAt(OffsetDateTime.now())
                        .build())
                .isDataReady(true)
                .hiddenSections(List.of("chat"))
                .readinessMetadata(Map.of("score", 0.9))
                .build();

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getSections().get(0).getSectionId()).isEqualTo("what");
        assertThat(response.getSections().get(0).getAnchor()).isEqualTo("what");
        assertThat(response.getCurrentCounters().getStars()).isEqualTo(100);
        assertThat(response.getHiddenSections()).containsExactly("chat");
    }
}
