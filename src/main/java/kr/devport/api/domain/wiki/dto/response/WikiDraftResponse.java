package kr.devport.api.domain.wiki.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiDraftResponse {

    private Long id;
    private Long projectId;
    private List<Map<String, Object>> sections;
    private Map<String, Object> currentCounters;
    private List<String> hiddenSections;
    private Long sourcePublishedVersionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
