package kr.devport.api.domain.wiki.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikiPublishedVersionTest {

    @Test
    @DisplayName("published version stores restore metadata and monotonic version number")
    void publishedVersionKeepsRestoreMetadata() {
        OffsetDateTime publishedAt = OffsetDateTime.parse("2026-02-16T11:00:00Z");

        WikiPublishedVersion version = WikiPublishedVersion.builder()
                .projectId(77L)
                .versionNumber(5)
                .publishedFromDraftId(23L)
                .rolledBackFromVersionId(4L)
                .publishedAt(publishedAt)
                .sections(List.of(Map.of(
                        "sectionId", "what",
                        "summary", "summary",
                        "deepDiveMarkdown", "deep"
                )))
                .currentCounters(Map.of("stars", 30))
                .hiddenSections(List.of("chat"))
                .build();

        assertThat(version.getProjectId()).isEqualTo(77L);
        assertThat(version.getVersionNumber()).isEqualTo(5);
        assertThat(version.getPublishedFromDraftId()).isEqualTo(23L);
        assertThat(version.getRolledBackFromVersionId()).isEqualTo(4L);
        assertThat(version.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(version.getSectionsOrEmpty()).hasSize(1);
        assertThat(version.getCurrentCounters()).containsEntry("stars", 30);
        assertThat(version.getHiddenSectionsOrEmpty()).containsExactly("chat");
    }
}
