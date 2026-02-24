package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.dto.response.WikiVersionHistoryResponse;
import kr.devport.api.domain.wiki.entity.WikiDraft;
import kr.devport.api.domain.wiki.entity.WikiPublishedVersion;
import kr.devport.api.domain.wiki.repository.WikiDraftRepository;
import kr.devport.api.domain.wiki.repository.WikiPublishedVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WikiPublishingService {

    private final WikiDraftRepository wikiDraftRepository;
    private final WikiPublishedVersionRepository wikiPublishedVersionRepository;

    public WikiVersionHistoryResponse publish(Long projectId, Long draftId) {
        WikiDraft draft = wikiDraftRepository.findByIdAndProjectId(draftId, projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Draft not found for project: projectId=" + projectId + ", draftId=" + draftId
                ));

        WikiPublishedVersion publishedVersion = WikiPublishedVersion.builder()
                .projectId(projectId)
                .versionNumber(nextVersionNumber(projectId))
                .sections(draft.getSectionsOrEmpty())
                .currentCounters(draft.getCurrentCounters())
                .hiddenSections(draft.getHiddenSectionsOrEmpty())
                .publishedFromDraftId(draftId)
                .rolledBackFromVersionId(null)
                .build();

        wikiPublishedVersionRepository.save(publishedVersion);
        return buildHistory(projectId);
    }

    public WikiVersionHistoryResponse rollbackToVersion(Long projectId, Integer targetVersionNumber) {
        WikiPublishedVersion targetVersion = wikiPublishedVersionRepository
                .findByProjectIdAndVersionNumber(projectId, targetVersionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Published version not found for rollback: projectId=" + projectId +
                                ", versionNumber=" + targetVersionNumber
                ));

        WikiPublishedVersion rollbackVersion = WikiPublishedVersion.builder()
                .projectId(projectId)
                .versionNumber(nextVersionNumber(projectId))
                .sections(targetVersion.getSectionsOrEmpty())
                .currentCounters(targetVersion.getCurrentCounters())
                .hiddenSections(targetVersion.getHiddenSectionsOrEmpty())
                .publishedFromDraftId(null)
                .rolledBackFromVersionId(targetVersion.getId())
                .build();

        wikiPublishedVersionRepository.save(rollbackVersion);
        return buildHistory(projectId);
    }

    private Integer nextVersionNumber(Long projectId) {
        return wikiPublishedVersionRepository.findTopByProjectIdOrderByVersionNumberDesc(projectId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    private WikiVersionHistoryResponse buildHistory(Long projectId) {
        List<WikiPublishedVersion> versions = wikiPublishedVersionRepository
                .findByProjectIdOrderByVersionNumberDesc(projectId);

        Integer latestVersionNumber = versions.isEmpty() ? null : versions.getFirst().getVersionNumber();
        List<WikiVersionHistoryResponse.PublishedVersionItem> items = versions.stream()
                .map(version -> WikiVersionHistoryResponse.PublishedVersionItem.builder()
                        .versionId(version.getId())
                        .versionNumber(version.getVersionNumber())
                        .publishedFromDraftId(version.getPublishedFromDraftId())
                        .rolledBackFromVersionId(version.getRolledBackFromVersionId())
                        .publishedAt(version.getPublishedAt())
                        .build())
                .toList();

        return WikiVersionHistoryResponse.builder()
                .projectId(projectId)
                .latestVersionNumber(latestVersionNumber)
                .versions(items)
                .build();
    }
}
