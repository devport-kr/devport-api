package kr.devport.api.domain.wiki;

import kr.devport.api.domain.wiki.dto.response.WikiDomainBrowseResponse;
import kr.devport.api.domain.wiki.dto.response.WikiProjectPageResponse;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for wiki API behavior.
 * <p>
 * Covers:
 * - Hide-incomplete section behavior (hiddenSections array)
 * - Domain browse filtering (data-ready projects only)
 * - Response structure alignment with locked UX decisions
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WikiApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProjectWikiSnapshotRepository wikiSnapshotRepository;

    @AfterEach
    void cleanup() {
        wikiSnapshotRepository.deleteAll();
    }

    @Test
    @DisplayName("wiki project page hides incomplete sections in hiddenSections array")
    void getProjectWiki_hidesIncompleteSections() {
        // Given: A project with incomplete architecture section
        ProjectWikiSnapshot snapshot = createTestSnapshot(
                "github:test/repo",
                "test-domain",
                true,  // isDataReady
                Map.of(
                        "what", Map.of("summary", "What summary", "deepDiveMarkdown", "What deep"),
                        "how", Map.of("summary", "How summary", "deepDiveMarkdown", "How deep"),
                        "architecture", null,  // Missing section
                        "activity", Map.of("summary", "Activity summary"),
                        "releases", Map.of("summary", "Release summary")
                )
        );
        wikiSnapshotRepository.save(snapshot);

        // When: Fetching project wiki
        ResponseEntity<WikiProjectPageResponse> response = restTemplate.getForEntity(
                "/api/wiki/projects/github:test/repo",
                WikiProjectPageResponse.class
        );

        // Then: Should return OK with architecture in hiddenSections
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getHiddenSections()).contains("architecture");
        assertThat(response.getBody().getWhat()).isNotNull();
        assertThat(response.getBody().getHow()).isNotNull();
        assertThat(response.getBody().getArchitecture()).isNull();  // Should be null, not present
    }

    @Test
    @DisplayName("domain browse returns only data-ready projects")
    void browseByDomain_returnsOnlyDataReadyProjects() {
        // Given: Mix of ready and not-ready projects in same domain
        ProjectWikiSnapshot readyProject = createTestSnapshot(
                "github:test/ready-repo",
                "ml-frameworks",
                true,  // isDataReady = true
                Map.of(
                        "what", Map.of("summary", "Ready summary"),
                        "activity", Map.of("summary", "Activity")
                )
        );
        
        ProjectWikiSnapshot notReadyProject = createTestSnapshot(
                "github:test/not-ready-repo",
                "ml-frameworks",
                false,  // isDataReady = false
                Map.of("what", Map.of("summary", "Not ready summary"))
        );

        wikiSnapshotRepository.saveAll(List.of(readyProject, notReadyProject));

        // When: Browsing domain
        ResponseEntity<WikiDomainBrowseResponse> response = restTemplate.getForEntity(
                "/api/wiki/domains/ml-frameworks",
                WikiDomainBrowseResponse.class
        );

        // Then: Should return only data-ready projects
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDomain()).isEqualTo("ml-frameworks");
        assertThat(response.getBody().getProjects()).hasSize(1);
        assertThat(response.getBody().getProjects().get(0).getProjectExternalId())
                .isEqualTo("github:test/ready-repo");
    }

    @Test
    @DisplayName("wiki response includes right-rail ordering metadata")
    void getProjectWiki_includesRightRailOrderingMetadata() {
        // Given: A complete wiki snapshot
        ProjectWikiSnapshot snapshot = createTestSnapshot(
                "github:test/complete-repo",
                "web-frameworks",
                true,
                Map.of(
                        "what", Map.of("summary", "What"),
                        "how", Map.of("summary", "How"),
                        "activity", Map.of("summary", "Activity summary"),
                        "releases", Map.of("summary", "Releases summary")
                )
        );
        wikiSnapshotRepository.save(snapshot);

        // When: Fetching project wiki
        ResponseEntity<WikiProjectPageResponse> response = restTemplate.getForEntity(
                "/api/wiki/projects/github:test/complete-repo",
                WikiProjectPageResponse.class
        );

        // Then: Response should include sections in expected order
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Activity and releases should be present (for right rail ordering)
        assertThat(response.getBody().getActivity()).isNotNull();
        assertThat(response.getBody().getReleases()).isNotNull();
        
        // Hidden sections should be minimal for complete project
        assertThat(response.getBody().getHiddenSections()).doesNotContain("activity", "releases");
    }

    @Test
    @DisplayName("project wiki returns 404 when snapshot not found")
    void getProjectWiki_returns404WhenNotFound() {
        // When: Fetching non-existent project
        ResponseEntity<WikiProjectPageResponse> response = restTemplate.getForEntity(
                "/api/wiki/projects/github:test/nonexistent",
                WikiProjectPageResponse.class
        );

        // Then: Should return 404
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("project wiki returns 404 when project not data-ready")
    void getProjectWiki_returns404WhenNotDataReady() {
        // Given: A project that is not data-ready
        ProjectWikiSnapshot snapshot = createTestSnapshot(
                "github:test/not-ready",
                "test-domain",
                false,  // isDataReady = false
                Map.of("what", Map.of("summary", "Summary"))
        );
        wikiSnapshotRepository.save(snapshot);

        // When: Fetching not-ready project
        ResponseEntity<WikiProjectPageResponse> response = restTemplate.getForEntity(
                "/api/wiki/projects/github:test/not-ready",
                WikiProjectPageResponse.class
        );

        // Then: Should return 404 (not-ready projects are not shown)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Helper method to create test snapshots
    private ProjectWikiSnapshot createTestSnapshot(
            String projectExternalId,
            String domain,
            boolean isDataReady,
            Map<String, Object> sections
    ) {
        ProjectWikiSnapshot snapshot = new ProjectWikiSnapshot();
        snapshot.setProjectExternalId(projectExternalId);
        snapshot.setDomain(domain);
        snapshot.setIsDataReady(isDataReady);
        snapshot.setGeneratedAt(LocalDateTime.now());
        
        // Set sections
        if (sections.containsKey("what")) {
            snapshot.setWhatSummary(getMapValue(sections, "what", "summary"));
            snapshot.setWhatDeepDiveMarkdown(getMapValue(sections, "what", "deepDiveMarkdown"));
        }
        if (sections.containsKey("how")) {
            snapshot.setHowSummary(getMapValue(sections, "how", "summary"));
            snapshot.setHowDeepDiveMarkdown(getMapValue(sections, "how", "deepDiveMarkdown"));
        }
        if (sections.containsKey("architecture")) {
            Object arch = sections.get("architecture");
            if (arch instanceof Map) {
                snapshot.setArchitectureSummary(getMapValue(sections, "architecture", "summary"));
                snapshot.setArchitectureDeepDiveMarkdown(getMapValue(sections, "architecture", "deepDiveMarkdown"));
            }
        }
        if (sections.containsKey("activity")) {
            snapshot.setActivitySummary(getMapValue(sections, "activity", "summary"));
            snapshot.setActivityDeepDiveMarkdown(getMapValue(sections, "activity", "deepDiveMarkdown"));
        }
        if (sections.containsKey("releases")) {
            snapshot.setReleasesSummary(getMapValue(sections, "releases", "summary"));
            snapshot.setReleasesDeepDiveMarkdown(getMapValue(sections, "releases", "deepDiveMarkdown"));
        }
        
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private String getMapValue(Map<String, Object> sections, String section, String key) {
        Object sectionData = sections.get(section);
        if (sectionData instanceof Map) {
            return (String) ((Map<String, Object>) sectionData).get(key);
        }
        return null;
    }
}
