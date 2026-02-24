package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.common.webhook.dto.CrawlerJobCompletedRequest;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.repository.ProjectWikiSnapshotRepository;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikiFreshnessSignalService {

    private static final Set<String> NARRATIVE_KEYS = Set.of(
            "sections",
            "deepDive",
            "deepDiveMarkdown"
    );

    private final ProjectWikiSnapshotRepository projectWikiSnapshotRepository;
    private final ProjectRepository projectRepository;
    private final WikiDraftRepository wikiDraftRepository;
    private final WikiAuthoringWorkflowService wikiAuthoringWorkflowService;

    @Transactional
    public void handleCrawlerCompletion(CrawlerJobCompletedRequest request) {
        for (Map<String, Object> signal : request.getFreshnessSignalsOrEmpty()) {
            if (containsNarrativePayload(signal)) {
                log.warn("Ignoring crawler narrative payload in freshness signal: jobId={}", request.getJobId());
                continue;
            }

            String projectExternalId = asText(signal.get("projectExternalId"));
            if (projectExternalId.isBlank()) {
                continue;
            }

            Map<String, Object> currentCounters = copyMap(signal.get("currentCounters"));
            Map<String, Object> readinessMetadata = copyMap(signal.get("readinessMetadata"));
            List<String> hiddenSections = asStringList(signal.get("hiddenSectionIds"));
            OffsetDateTime generatedAt = parseTimestamp(signal.get("generatedAt"), request.getCompletedAt());

            upsertFreshnessSnapshot(projectExternalId, generatedAt, currentCounters, readinessMetadata, hiddenSections);
            triggerDraftRegeneration(projectExternalId, currentCounters, hiddenSections);
        }
    }

    private void upsertFreshnessSnapshot(
            String projectExternalId,
            OffsetDateTime generatedAt,
            Map<String, Object> currentCounters,
            Map<String, Object> readinessMetadata,
            List<String> hiddenSections
    ) {
        ProjectWikiSnapshot snapshot = projectWikiSnapshotRepository.findByProjectExternalId(projectExternalId)
                .orElseGet(() -> ProjectWikiSnapshot.builder()
                        .projectExternalId(projectExternalId)
                        .generatedAt(generatedAt)
                        .sections(List.of())
                        .currentCounters(Map.of())
                        .isDataReady(Boolean.TRUE.equals(readinessMetadata.get("passesTopStarGate")))
                        .hiddenSections(List.of())
                        .readinessMetadata(Map.of())
                        .build());

        snapshot.applyFreshnessSignal(generatedAt, currentCounters, readinessMetadata, hiddenSections);
        projectWikiSnapshotRepository.save(snapshot);
    }

    private void triggerDraftRegeneration(
            String projectExternalId,
            Map<String, Object> currentCounters,
            List<String> hiddenSections
    ) {
        Project project = projectRepository.findByExternalId(projectExternalId).orElse(null);
        if (project == null) {
            return;
        }

        WikiDraft latestDraft = wikiDraftRepository.findTopByProjectIdOrderByUpdatedAtDesc(project.getId()).orElse(null);
        if (latestDraft == null) {
            return;
        }

        wikiAuthoringWorkflowService.regenerateDraft(
                project.getId(),
                latestDraft.getId(),
                latestDraft.getSectionsOrEmpty(),
                currentCounters,
                hiddenSections
        );
    }

    private boolean containsNarrativePayload(Map<String, Object> signal) {
        if (signal == null || signal.isEmpty()) {
            return false;
        }
        return containsNarrativePayloadInObject(signal);
    }

    @SuppressWarnings("unchecked")
    private boolean containsNarrativePayloadInObject(Object node) {
        if (node instanceof Map<?, ?> mapNode) {
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (NARRATIVE_KEYS.contains(key)) {
                    return true;
                }
                if (containsNarrativePayloadInObject(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        if (node instanceof List<?> listNode) {
            for (Object item : listNode) {
                if (containsNarrativePayloadInObject(item)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    private Map<String, Object> copyMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, Object> copied = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> copied.put(String.valueOf(key), mapValue));
        return copied;
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        return rawList.stream().map(String::valueOf).toList();
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private OffsetDateTime parseTimestamp(Object preferred, String fallback) {
        if (preferred != null) {
            try {
                return OffsetDateTime.parse(String.valueOf(preferred));
            } catch (DateTimeParseException ignored) {
            }
        }

        if (fallback != null && !fallback.isBlank()) {
            try {
                return OffsetDateTime.parse(fallback);
            } catch (DateTimeParseException ignored) {
            }
        }
        return OffsetDateTime.now();
    }
}
