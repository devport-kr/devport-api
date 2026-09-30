package kr.devport.api.domain.llm.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.devport.api.domain.llm.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMBenchmarkCreateRequest {

    @NotNull(message = "Benchmark type is required")
    private BenchmarkType benchmarkType;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Category group is required")
    private String categoryGroup;

    @NotBlank(message = "Description is required")
    private String description;

    private String explanation;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
}
