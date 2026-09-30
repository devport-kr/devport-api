package kr.devport.api.domain.llm.dto.request.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMBenchmarkUpdateRequest {

    private String displayName;
    private String categoryGroup;
    private String description;
    private String explanation;
    private Integer sortOrder;
}
