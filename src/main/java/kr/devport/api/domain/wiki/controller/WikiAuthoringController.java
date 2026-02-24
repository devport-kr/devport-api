package kr.devport.api.domain.wiki.controller;

import jakarta.validation.Valid;
import kr.devport.api.domain.wiki.dto.request.WikiDraftUpsertRequest;
import kr.devport.api.domain.wiki.dto.request.WikiPublishRequest;
import kr.devport.api.domain.wiki.dto.request.WikiRollbackRequest;
import kr.devport.api.domain.wiki.dto.response.WikiDraftResponse;
import kr.devport.api.domain.wiki.dto.response.WikiVersionHistoryResponse;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.service.WikiAuthoringWorkflowService;
import kr.devport.api.domain.wiki.service.WikiPublishingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wiki/admin/projects/{projectId}")
@RequiredArgsConstructor
public class WikiAuthoringController {

    private final WikiAuthoringWorkflowService wikiAuthoringWorkflowService;
    private final WikiPublishingService wikiPublishingService;

    @PostMapping("/drafts")
    public ResponseEntity<WikiDraftResponse> createDraft(
            @PathVariable Long projectId,
            @Valid @RequestBody WikiDraftUpsertRequest request
    ) {
        WikiDraft created = wikiAuthoringWorkflowService.createDraft(
                projectId,
                request.getSections(),
                request.getCurrentCounters(),
                request.getHiddenSections(),
                request.getSourcePublishedVersionId()
        );
        return ResponseEntity.ok(toDraftResponse(created));
    }

    @GetMapping("/drafts")
    public ResponseEntity<List<WikiDraftResponse>> listDrafts(@PathVariable Long projectId) {
        List<WikiDraftResponse> drafts = wikiAuthoringWorkflowService.listDrafts(projectId).stream()
                .map(this::toDraftResponse)
                .toList();
        return ResponseEntity.ok(drafts);
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<WikiDraftResponse> getDraft(
            @PathVariable Long projectId,
            @PathVariable Long draftId
    ) {
        WikiDraft draft = wikiAuthoringWorkflowService.getDraft(projectId, draftId);
        return ResponseEntity.ok(toDraftResponse(draft));
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<WikiDraftResponse> updateDraft(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @Valid @RequestBody WikiDraftUpsertRequest request
    ) {
        WikiDraft updated = wikiAuthoringWorkflowService.updateDraft(
                projectId,
                draftId,
                request.getSections(),
                request.getCurrentCounters(),
                request.getHiddenSections()
        );
        return ResponseEntity.ok(toDraftResponse(updated));
    }

    @PostMapping("/drafts/{draftId}/regenerate")
    public ResponseEntity<WikiDraftResponse> regenerateDraft(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @Valid @RequestBody WikiDraftUpsertRequest request
    ) {
        WikiDraft regenerated = wikiAuthoringWorkflowService.regenerateDraft(
                projectId,
                draftId,
                request.getSections(),
                request.getCurrentCounters(),
                request.getHiddenSections()
        );
        return ResponseEntity.ok(toDraftResponse(regenerated));
    }

    @PostMapping("/publish")
    public ResponseEntity<WikiVersionHistoryResponse> publish(
            @PathVariable Long projectId,
            @Valid @RequestBody WikiPublishRequest request
    ) {
        WikiVersionHistoryResponse response = wikiPublishingService.publish(projectId, request.getDraftId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rollback")
    public ResponseEntity<WikiVersionHistoryResponse> rollback(
            @PathVariable Long projectId,
            @Valid @RequestBody WikiRollbackRequest request
    ) {
        WikiVersionHistoryResponse response = wikiPublishingService.rollbackToVersion(
                projectId,
                request.getTargetVersionNumber()
        );
        return ResponseEntity.ok(response);
    }

    private WikiDraftResponse toDraftResponse(WikiDraft draft) {
        return WikiDraftResponse.builder()
                .id(draft.getId())
                .projectId(draft.getProjectId())
                .sections(draft.getSectionsOrEmpty())
                .currentCounters(draft.getCurrentCounters())
                .hiddenSections(draft.getHiddenSectionsOrEmpty())
                .sourcePublishedVersionId(draft.getSourcePublishedVersionId())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}
