package kr.devport.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "LLM ranking response with benchmark info and model scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMRankingResponse {

    @Schema(description = "Benchmark information")
    private BenchmarkResponse benchmark;

    @Schema(description = "List of LLM models with scores")
    private List<LLMModelResponse> models;
}
