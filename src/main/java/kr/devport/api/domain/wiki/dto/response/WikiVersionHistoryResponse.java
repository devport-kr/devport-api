package kr.devport.api.domain.wiki.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiVersionHistoryResponse {

    private Long projectId;
    private Integer latestVersionNumber;
    private List<PublishedVersionItem> versions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishedVersionItem {
        private Long versionId;
        private Integer versionNumber;
        private Long publishedFromDraftId;
        private Long rolledBackFromVersionId;
        private OffsetDateTime publishedAt;
    }
}
