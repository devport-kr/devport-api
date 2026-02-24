package kr.devport.api.domain.wiki.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikiProjectPageResponseTest {

    @Test
    @DisplayName("project page response supports dynamic sections anchors and current counters")
    void builder_supportsDynamicResponseShape() {
        WikiProjectPageResponse response = WikiProjectPageResponse.builder()
                .projectExternalId("github:repo")
                .fullName("owner/repo")
                .generatedAt(OffsetDateTime.now())
                .sections(List.of(
                        WikiProjectPageResponse.WikiSection.builder()
                                .sectionId("architecture")
                                .heading("Architecture")
                                .anchor("architecture")
                                .summary("summary")
                                .deepDiveMarkdown("deep")
                                .defaultExpanded(false)
                                .metadata(Map.of("category", "core"))
                                .build()
                ))
                .anchors(List.of(
                        WikiProjectPageResponse.AnchorItem.builder()
                                .sectionId("architecture")
                                .heading("Architecture")
                                .anchor("architecture")
                                .build()
                ))
                .currentCounters(WikiProjectPageResponse.CurrentCounters.builder()
                        .stars(100)
                        .forks(20)
                        .watchers(5)
                        .openIssues(2)
                        .updatedAt(OffsetDateTime.now())
                        .build())
                .build();

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getAnchors()).hasSize(1);
        assertThat(response.getAnchors().get(0).getSectionId()).isEqualTo("architecture");
        assertThat(response.getCurrentCounters().getStars()).isEqualTo(100);
    }
}
