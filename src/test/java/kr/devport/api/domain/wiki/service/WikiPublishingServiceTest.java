package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.dto.response.WikiVersionHistoryResponse;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.entity.WikiPublishedVersion;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import kr.devport.api.domain.wiki.repository.WikiPublishedVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiPublishingServiceTest {

    @Mock
    private WikiDraftRepository wikiDraftRepository;

    @Mock
    private WikiPublishedVersionRepository wikiPublishedVersionRepository;

    @InjectMocks
    private WikiPublishingService wikiPublishingService;

    @Test
    @DisplayName("publish snapshots selected draft into a new immutable published version")
    void publish_createsNextVersionFromDraft() {
        WikiDraft draft = WikiDraft.builder()
                .projectId(9L)
                .sections(List.of(Map.of(
                        "sectionId", "what",
                        "summary", "summary",
                        "deepDiveMarkdown", "deep"
                )))
                .currentCounters(Map.of("stars", 10))
                .hiddenSections(List.of("chat"))
                .build();
        ReflectionTestUtils.setField(draft, "id", 12L);

        WikiPublishedVersion latest = WikiPublishedVersion.builder()
                .projectId(9L)
                .versionNumber(2)
                .publishedAt(OffsetDateTime.parse("2026-02-16T00:00:00Z"))
                .build();

        WikiPublishedVersion newVersion = WikiPublishedVersion.builder()
                .projectId(9L)
                .versionNumber(3)
                .publishedFromDraftId(12L)
                .publishedAt(OffsetDateTime.parse("2026-02-16T00:10:00Z"))
                .build();

        when(wikiDraftRepository.findByIdAndProjectId(12L, 9L)).thenReturn(Optional.of(draft));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(9L)).thenReturn(Optional.of(latest));
        when(wikiPublishedVersionRepository.save(any(WikiPublishedVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wikiPublishedVersionRepository.findByProjectIdOrderByVersionNumberDesc(9L))
                .thenReturn(List.of(newVersion, latest));

        WikiVersionHistoryResponse response = wikiPublishingService.publish(9L, 12L);

        ArgumentCaptor<WikiPublishedVersion> captor = ArgumentCaptor.forClass(WikiPublishedVersion.class);
        verify(wikiPublishedVersionRepository).save(captor.capture());
        WikiPublishedVersion saved = captor.getValue();

        assertThat(saved.getVersionNumber()).isEqualTo(3);
        assertThat(saved.getPublishedFromDraftId()).isEqualTo(12L);
        assertThat(saved.getRolledBackFromVersionId()).isNull();
        assertThat(saved.getSectionsOrEmpty()).hasSize(1);
        assertThat(saved.getCurrentCounters()).containsEntry("stars", 10);

        assertThat(response.getProjectId()).isEqualTo(9L);
        assertThat(response.getLatestVersionNumber()).isEqualTo(3);
        assertThat(response.getVersions()).hasSize(2);
        assertThat(response.getVersions().get(0).getVersionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("rollback can target any historical published version and re-promote as newest")
    void rollbackToVersion_repromotesHistoricalVersion() {
        WikiPublishedVersion latest = WikiPublishedVersion.builder()
                .projectId(9L)
                .versionNumber(5)
                .publishedAt(OffsetDateTime.parse("2026-02-16T00:00:00Z"))
                .build();

        WikiPublishedVersion target = WikiPublishedVersion.builder()
                .projectId(9L)
                .versionNumber(2)
                .sections(List.of(Map.of(
                        "sectionId", "architecture",
                        "summary", "restored",
                        "deepDiveMarkdown", "restored deep"
                )))
                .currentCounters(Map.of("stars", 3))
                .hiddenSections(List.of("activity"))
                .publishedAt(OffsetDateTime.parse("2026-02-10T00:00:00Z"))
                .build();
        ReflectionTestUtils.setField(target, "id", 200L);

        WikiPublishedVersion rollback = WikiPublishedVersion.builder()
                .projectId(9L)
                .versionNumber(6)
                .rolledBackFromVersionId(200L)
                .publishedAt(OffsetDateTime.parse("2026-02-16T00:10:00Z"))
                .build();

        when(wikiPublishedVersionRepository.findByProjectIdAndVersionNumber(9L, 2)).thenReturn(Optional.of(target));
        when(wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(9L)).thenReturn(Optional.of(latest));
        when(wikiPublishedVersionRepository.save(any(WikiPublishedVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wikiPublishedVersionRepository.findByProjectIdOrderByVersionNumberDesc(9L))
                .thenReturn(List.of(rollback, latest, target));

        WikiVersionHistoryResponse response = wikiPublishingService.rollbackToVersion(9L, 2);

        ArgumentCaptor<WikiPublishedVersion> captor = ArgumentCaptor.forClass(WikiPublishedVersion.class);
        verify(wikiPublishedVersionRepository).save(captor.capture());
        WikiPublishedVersion saved = captor.getValue();

        assertThat(saved.getVersionNumber()).isEqualTo(6);
        assertThat(saved.getPublishedFromDraftId()).isNull();
        assertThat(saved.getRolledBackFromVersionId()).isEqualTo(target.getId());
        assertThat(saved.getSectionsOrEmpty().get(0).get("summary")).isEqualTo("restored");
        assertThat(saved.getHiddenSectionsOrEmpty()).containsExactly("activity");

        assertThat(response.getLatestVersionNumber()).isEqualTo(6);
        assertThat(response.getVersions()).hasSize(3);
    }
}
