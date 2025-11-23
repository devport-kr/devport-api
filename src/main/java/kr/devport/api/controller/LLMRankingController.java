package kr.devport.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.enums.BenchmarkType;
import kr.devport.api.dto.response.BenchmarkResponse;
import kr.devport.api.dto.response.LLMRankingResponse;
import kr.devport.api.service.LLMRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "LLM Rankings", description = "LLM benchmark rankings and performance metrics (public)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LLMRankingController {

    private final LLMRankingService llmRankingService;

    @Operation(
        summary = "Get LLM benchmark rankings",
        description = "Retrieve LLM model rankings for a specific benchmark type with configurable result limit"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved LLM rankings",
            content = @Content(schema = @Schema(implementation = LLMRankingResponse.class))
        )
    })
    @GetMapping("/llm-rankings")
    public ResponseEntity<LLMRankingResponse> getLLMRankings(
        @Parameter(description = "Benchmark type: AGENTIC_CODING, REASONING, MATH, VISUAL, MULTILINGUAL")
        @RequestParam(defaultValue = "AGENTIC_CODING") BenchmarkType benchmark,
        @Parameter(description = "Maximum number of models to return")
        @RequestParam(defaultValue = "8") int limit
    ) {
        LLMRankingResponse response = llmRankingService.getLLMRankings(benchmark, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all benchmark configurations",
        description = "Retrieve all available benchmark types and their configurations (reference data)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved benchmark configurations"
        )
    })
    @GetMapping("/benchmarks")
    public ResponseEntity<List<BenchmarkResponse>> getAllBenchmarks() {
        List<BenchmarkResponse> response = llmRankingService.getAllBenchmarks();
        return ResponseEntity.ok(response);
    }
}
