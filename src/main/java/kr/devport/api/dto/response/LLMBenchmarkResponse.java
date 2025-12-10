package kr.devport.api.dto.response;

import kr.devport.api.domain.entity.LLMBenchmark;
import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 프론트 노출용 벤치마크 메타데이터 DTO. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMBenchmarkResponse {

    private BenchmarkType benchmarkType;
    private String displayName;
    private String categoryGroup;
    private String description;
    private String explanation;
    private Integer sortOrder;

    public static LLMBenchmarkResponse fromEntity(LLMBenchmark benchmark) {
        return LLMBenchmarkResponse.builder()
            .benchmarkType(benchmark.getBenchmarkType())
            .displayName(benchmark.getDisplayName())
            .categoryGroup(benchmark.getCategoryGroup())
            .description(benchmark.getDescription())
            .explanation(benchmark.getExplanation())
            .sortOrder(benchmark.getSortOrder())
            .build();
    }
}
