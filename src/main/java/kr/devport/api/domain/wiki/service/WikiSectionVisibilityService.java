package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enforces hide-incomplete policy on dynamic wiki section payloads.
 */
@Service
@RequiredArgsConstructor
public class WikiSectionVisibilityService {

    /**
     * Returns dynamic sections that are visible and renderable.
     */
    public List<Map<String, Object>> getVisibleSectionData(ProjectWikiSnapshot snapshot) {
        return getVisibleSectionData(snapshot.getResolvedSections(), snapshot.getHiddenSectionsOrEmpty());
    }

    /**
     * Returns dynamic sections that are visible and renderable using explicit payload values.
     */
    public List<Map<String, Object>> getVisibleSectionData(
            List<Map<String, Object>> sectionPayload,
            List<String> hiddenSections
    ) {
        List<Map<String, Object>> visibleSections = new ArrayList<>();
        if (sectionPayload == null || sectionPayload.isEmpty()) {
            return visibleSections;
        }
        List<String> hidden = hiddenSections == null ? List.of() : hiddenSections;
        for (Map<String, Object> sectionData : sectionPayload) {
            String sectionId = extractSectionId(sectionData);
            if (!isSectionReady(sectionData, sectionId, hidden)) {
                continue;
            }
            visibleSections.add(sectionData);
        }
        return visibleSections;
    }

    /**
     * Returns only visible section identifiers for anchor composition.
     */
    public List<String> getVisibleSections(ProjectWikiSnapshot snapshot) {
        return getVisibleSectionData(snapshot.getResolvedSections(), snapshot.getHiddenSectionsOrEmpty()).stream()
                .map(this::extractSectionId)
                .collect(Collectors.toList());
    }

    /**
     * Returns only visible section identifiers for anchor composition from explicit payload values.
     */
    public List<String> getVisibleSections(List<Map<String, Object>> sectionPayload, List<String> hiddenSections) {
        return getVisibleSectionData(sectionPayload, hiddenSections).stream()
                .map(this::extractSectionId)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a specific section should be visible in the response.
     *
     * @param snapshot Wiki snapshot with readiness metadata
     * @param sectionName Section name to check (e.g., "architecture")
     * @return true if section should be included in response
     */
    public boolean isSectionVisible(ProjectWikiSnapshot snapshot, String sectionName) {
        return isSectionVisible(snapshot.getResolvedSections(), snapshot.getHiddenSectionsOrEmpty(), sectionName);
    }

    /**
     * Checks if a specific section should be visible in response using explicit payload values.
     */
    public boolean isSectionVisible(
            List<Map<String, Object>> sectionPayload,
            List<String> hiddenSections,
            String sectionName
    ) {
        if (sectionPayload == null || sectionPayload.isEmpty()) {
            return false;
        }
        if (hiddenSections != null && hiddenSections.contains(sectionName)) {
            return false;
        }

        return sectionPayload.stream()
                .filter(section -> sectionName.equals(extractSectionId(section)))
                .anyMatch(section -> isSectionReady(section, sectionName, hiddenSections == null ? List.of() : hiddenSections));
    }

    /**
     * Checks if a section has content and is ready to render.
     * A section is ready if it's not hidden and has non-empty summary content.
     *
     * @param sectionData Section JSON data
     * @param sectionName Section name for hidden check
     * @param snapshot Snapshot for hidden sections list
     * @return true if section is ready to render
     */
    public boolean isSectionReady(Map<String, Object> sectionData, String sectionName, ProjectWikiSnapshot snapshot) {
        return isSectionReady(sectionData, sectionName, snapshot.getHiddenSectionsOrEmpty());
    }

    public boolean isSectionReady(Map<String, Object> sectionData, String sectionName, List<String> hiddenSections) {
        if (hiddenSections.contains(sectionName)) {
            return false;
        }

        if (sectionData == null || sectionData.isEmpty()) {
            return false;
        }

        String summary = asText(sectionData.get("summary"));
        String deepDive = asText(sectionData.getOrDefault("deepDiveMarkdown", sectionData.get("deepDive")));
        if (summary.isBlank() || deepDive.isBlank()) {
            return false;
        }

        if (Boolean.TRUE.equals(sectionData.get("uncertain"))) {
            return false;
        }

        Object metadata = sectionData.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap) {
            Object uncertain = metadataMap.get("uncertain");
            if (Boolean.TRUE.equals(uncertain)) {
                return false;
            }
        }

        return true;
    }

    private String extractSectionId(Map<String, Object> sectionData) {
        if (sectionData == null) {
            return "";
        }

        Object sectionId = sectionData.get("sectionId");
        if (sectionId == null) {
            return "";
        }
        return String.valueOf(sectionId);
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
