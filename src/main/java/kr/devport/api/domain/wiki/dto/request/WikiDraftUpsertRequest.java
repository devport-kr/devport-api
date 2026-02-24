package kr.devport.api.domain.wiki.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiDraftUpsertRequest {

    @Builder.Default
    private List<Map<String, Object>> sections = List.of();

    @Builder.Default
    private Map<String, Object> currentCounters = Map.of();

    @Builder.Default
    private List<String> hiddenSections = List.of();

    private Long sourcePublishedVersionId;
}
