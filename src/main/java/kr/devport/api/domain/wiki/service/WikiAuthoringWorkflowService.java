package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WikiAuthoringWorkflowService {

    private final WikiDraftRepository wikiDraftRepository;

    public WikiDraft createDraft(
            Long projectId,
            List<Map<String, Object>> sections,
            Map<String, Object> currentCounters,
            List<String> hiddenSections,
            Long sourcePublishedVersionId
    ) {
        WikiDraft draft = WikiDraft.builder()
                .projectId(projectId)
                .sections(normalizeSections(sections))
                .currentCounters(copyMap(currentCounters))
                .hiddenSections(hiddenSections == null ? List.of() : new ArrayList<>(hiddenSections))
                .sourcePublishedVersionId(sourcePublishedVersionId)
                .build();
        return wikiDraftRepository.save(draft);
    }

    @Transactional(readOnly = true)
    public List<WikiDraft> listDrafts(Long projectId) {
        return wikiDraftRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public WikiDraft getDraft(Long projectId, Long draftId) {
        return findDraft(projectId, draftId);
    }

    public WikiDraft updateDraft(
            Long projectId,
            Long draftId,
            List<Map<String, Object>> sections,
            Map<String, Object> currentCounters,
            List<String> hiddenSections
    ) {
        WikiDraft draft = findDraft(projectId, draftId);
        draft.replaceSections(normalizeSections(sections));
        draft.replaceCurrentCounters(copyMap(currentCounters));
        draft.replaceHiddenSections(hiddenSections == null ? List.of() : new ArrayList<>(hiddenSections));
        return wikiDraftRepository.save(draft);
    }

    public WikiDraft regenerateDraft(
            Long projectId,
            Long draftId,
            List<Map<String, Object>> regeneratedSections,
            Map<String, Object> currentCounters,
            List<String> hiddenSections
    ) {
        WikiDraft draft = findDraft(projectId, draftId);

        draft.replaceSections(normalizeSections(regeneratedSections));
        draft.replaceCurrentCounters(copyMap(currentCounters));
        draft.replaceHiddenSections(hiddenSections == null ? List.of() : new ArrayList<>(hiddenSections));
        return wikiDraftRepository.save(draft);
    }

    public WikiDraft regenerateDraft(
            Long draftId,
            List<Map<String, Object>> regeneratedSections,
            Map<String, Object> currentCounters,
            List<String> hiddenSections
    ) {
        WikiDraft draft = wikiDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        draft.replaceSections(normalizeSections(regeneratedSections));
        draft.replaceCurrentCounters(copyMap(currentCounters));
        draft.replaceHiddenSections(hiddenSections == null ? List.of() : new ArrayList<>(hiddenSections));
        return wikiDraftRepository.save(draft);
    }

    public WikiDraft editDraftSection(
            Long draftId,
            String sectionId,
            String summary,
            String deepDiveMarkdown
    ) {
        WikiDraft draft = wikiDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        List<Map<String, Object>> updatedSections = normalizeSections(draft.getSectionsOrEmpty());
        String normalizedSectionId = safeText(sectionId);

        boolean updated = false;
        for (Map<String, Object> section : updatedSections) {
            if (normalizedSectionId.equals(safeText(section.get("sectionId")))) {
                section.put("summary", safeText(summary));
                section.put("deepDiveMarkdown", safeText(deepDiveMarkdown));
                updated = true;
                break;
            }
        }

        if (!updated) {
            Map<String, Object> section = new LinkedHashMap<>();
            section.put("sectionId", normalizedSectionId);
            section.put("heading", normalizedSectionId);
            section.put("anchor", normalizedSectionId);
            section.put("summary", safeText(summary));
            section.put("deepDiveMarkdown", safeText(deepDiveMarkdown));
            updatedSections.add(section);
        }

        draft.replaceSections(updatedSections);
        return wikiDraftRepository.save(draft);
    }

    private List<Map<String, Object>> normalizeSections(List<Map<String, Object>> sections) {
        if (sections == null) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>(sections.size());
        for (Map<String, Object> section : sections) {
            if (section == null) {
                continue;
            }

            Map<String, Object> normalizedSection = new LinkedHashMap<>(section);
            String sectionId = safeText(normalizedSection.get("sectionId"));
            normalizedSection.put("sectionId", sectionId);
            normalizedSection.putIfAbsent("heading", sectionId);
            normalizedSection.putIfAbsent("anchor", sectionId);
            normalizedSection.put("summary", safeText(normalizedSection.get("summary")));

            Object deepDiveValue = normalizedSection.getOrDefault("deepDiveMarkdown", normalizedSection.get("deepDive"));
            normalizedSection.put("deepDiveMarkdown", safeText(deepDiveValue));
            normalizedSection.remove("deepDive");
            normalized.add(normalizedSection);
        }
        return normalized;
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private WikiDraft findDraft(Long projectId, Long draftId) {
        return wikiDraftRepository.findByIdAndProjectId(draftId, projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Draft not found for project: projectId=" + projectId + ", draftId=" + draftId
                ));
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
