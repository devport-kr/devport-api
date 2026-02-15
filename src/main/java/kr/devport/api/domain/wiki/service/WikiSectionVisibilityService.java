package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enforces hide-incomplete section policy at response composition time.
 * Removes missing/uncertain sections from response output instead of returning empty placeholders.
 * Preserves anchored navigation stability by only exposing visible section IDs.
 */
@Service
@RequiredArgsConstructor
public class WikiSectionVisibilityService {

    /**
     * Returns list of section IDs that are visible (not in hiddenSections).
     * Used for anchor navigation and UI rendering decisions.
     *
     * @param snapshot Wiki snapshot with readiness metadata
     * @return List of visible section IDs (e.g., ["what", "how", "architecture"])
     */
    public List<String> getVisibleSections(ProjectWikiSnapshot snapshot) {
        List<String> allSections = List.of("what", "how", "architecture", "activity", "releases");
        List<String> hiddenSections = snapshot.getHiddenSections();
        
        List<String> visibleSections = new ArrayList<>();
        for (String section : allSections) {
            if (!hiddenSections.contains(section)) {
                visibleSections.add(section);
            }
        }
        
        return visibleSections;
    }

    /**
     * Checks if a specific section should be visible in the response.
     *
     * @param snapshot Wiki snapshot with readiness metadata
     * @param sectionName Section name to check (e.g., "architecture")
     * @return true if section should be included in response
     */
    public boolean isSectionVisible(ProjectWikiSnapshot snapshot, String sectionName) {
        return !snapshot.getHiddenSections().contains(sectionName);
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
        if (snapshot.getHiddenSections().contains(sectionName)) {
            return false;
        }
        
        if (sectionData == null || sectionData.isEmpty()) {
            return false;
        }
        
        Object summary = sectionData.get("summary");
        return summary != null && !summary.toString().isBlank();
    }
}
