package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiRetrievalServiceTest {

    @Mock
    private ProjectWikiSnapshotRepository wikiSnapshotRepository;

    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private WikiRetrievalService wikiRetrievalService;

    private ProjectWikiSnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        testSnapshot = ProjectWikiSnapshot.builder()
                .projectExternalId("github:12345")
                .generatedAt(OffsetDateTime.now())
                .whatSection(Map.of(
                        "summary", "Test project for testing",
                        "deepDiveMarkdown", "This is a deep dive into test project"
                ))
                .howSection(Map.of(
                        "summary", "How it works summary",
                        "deepDiveMarkdown", "Detailed how it works content"
                ))
                .architectureSection(Map.of(
                        "summary", "Architecture overview",
                        "deepDiveMarkdown", "Detailed architecture explanation"
                ))
                .activitySection(Map.of(
                        "summary", "Recent activity"
                ))
                .releasesSection(Map.of(
                        "summary", "Latest releases"
                ))
                .chatSection(Map.of())
                .isDataReady(true)
                .hiddenSections(List.of())
                .readinessMetadata(Map.of())
                .build();
    }

    @Test
    @DisplayName("retrieveContext combines repo context from wiki sections")
    void retrieveContext_combinesRepoContext() {
        // Given
        String projectExternalId = "github:12345";
        String userQuestion = "How does this project work?";

        when(wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady(projectExternalId, true))
                .thenReturn(Optional.of(testSnapshot));

        // When
        String context = wikiRetrievalService.retrieveContext(projectExternalId, userQuestion);

        // Then
        assertThat(context).isNotBlank();
        assertThat(context).contains("# Repository Context");
        assertThat(context).contains("Architecture overview");
        assertThat(context).contains("How it works summary");
        assertThat(context).contains("Test project for testing");
    }

    @Test
    @DisplayName("retrieveContext throws exception when snapshot not ready")
    void retrieveContext_throwsExceptionWhenSnapshotNotReady() {
        // Given
        String projectExternalId = "github:12345";
        String userQuestion = "How does this project work?";

        when(wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady(projectExternalId, true))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> wikiRetrievalService.retrieveContext(projectExternalId, userQuestion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wiki snapshot not ready");
    }

    @Test
    @DisplayName("retrieveContext handles sections with only summaries")
    void retrieveContext_handlesSectionsWithOnlySummaries() {
        // Given
        String projectExternalId = "github:12345";
        String userQuestion = "What is this project?";

        ProjectWikiSnapshot minimalSnapshot = ProjectWikiSnapshot.builder()
                .projectExternalId(projectExternalId)
                .generatedAt(OffsetDateTime.now())
                .whatSection(Map.of("summary", "Minimal summary"))
                .howSection(Map.of())
                .architectureSection(Map.of())
                .activitySection(Map.of())
                .releasesSection(Map.of())
                .chatSection(Map.of())
                .isDataReady(true)
                .hiddenSections(List.of())
                .readinessMetadata(Map.of())
                .build();

        when(wikiSnapshotRepository.findByProjectExternalIdAndIsDataReady(projectExternalId, true))
                .thenReturn(Optional.of(minimalSnapshot));

        // When
        String context = wikiRetrievalService.retrieveContext(projectExternalId, userQuestion);

        // Then
        assertThat(context).isNotBlank();
        assertThat(context).contains("Minimal summary");
    }
}
