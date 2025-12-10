package kr.devport.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.enums.BenchmarkType;
import kr.devport.api.dto.response.*;
import kr.devport.api.service.LLMRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "LLM Rankings", description = "LLM benchmark rankings and performance metrics (public)")
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LLMRankingController {

    private final LLMRankingService llmRankingService;

    @Operation(
        summary = "Get all LLM models",
        description = "Retrieve all LLM models with optional filters for provider, license, price, and context window. Paginated results sorted by Intelligence Index."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved LLM models",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    @GetMapping("/models")
    public ResponseEntity<Page<LLMModelSummaryResponse>> getAllModels(
        @Parameter(description = "Filter by provider (legacy field, e.g., 'OpenAI', 'Anthropic', 'Google')")
        @RequestParam(required = false) String provider,

        @Parameter(description = "Filter by model creator slug (e.g., 'openai', 'anthropic', 'alibaba')")
        @RequestParam(required = false) String creatorSlug,

        @Parameter(description = "Filter by license ('Open' or 'Proprietary')")
        @RequestParam(required = false) String license,

        @Parameter(description = "Maximum blended price (USD per 1M tokens)")
        @RequestParam(required = false) BigDecimal maxPrice,

        @Parameter(description = "Minimum context window (tokens)")
        @RequestParam(required = false) Long minContextWindow,

        @PageableDefault(size = 20, sort = "scoreAaIntelligenceIndex") Pageable pageable
    ) {
        Page<LLMModelSummaryResponse> models = llmRankingService.getAllModels(
            provider, creatorSlug, license, maxPrice, minContextWindow, pageable
        );
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Get LLM model details",
        description = "Retrieve detailed information about a specific LLM model including all benchmark scores"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved model details",
            content = @Content(schema = @Schema(implementation = LLMModelDetailResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Model not found"
        )
    })
    @GetMapping("/models/{modelId}")
    public ResponseEntity<LLMModelDetailResponse> getModelById(
        @Parameter(description = "Model ID (e.g., 'gpt-4-turbo', 'claude-opus-4-5')")
        @PathVariable String modelId
    ) {
        LLMModelDetailResponse model = llmRankingService.getModelById(modelId);
        return ResponseEntity.ok(model);
    }


    @Operation(
        summary = "Get benchmark leaderboard",
        description = "Retrieve leaderboard rankings for a specific benchmark type with optional filters. Ranks are calculated dynamically."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved leaderboard",
            content = @Content(schema = @Schema(implementation = LLMLeaderboardEntryResponse.class))
        )
    })
    @GetMapping("/leaderboard/{benchmarkType}")
    public ResponseEntity<List<LLMLeaderboardEntryResponse>> getLeaderboard(
        @Parameter(description = "Benchmark type", example = "AA_INTELLIGENCE_INDEX")
        @PathVariable BenchmarkType benchmarkType,

        @Parameter(description = "Filter by provider (legacy field)")
        @RequestParam(required = false) String provider,

        @Parameter(description = "Filter by model creator slug (e.g., 'openai', 'anthropic', 'alibaba')")
        @RequestParam(required = false) String creatorSlug,

        @Parameter(description = "Filter by license ('Open' or 'Proprietary')")
        @RequestParam(required = false) String license,

        @Parameter(description = "Maximum blended price (USD per 1M tokens)")
        @RequestParam(required = false) BigDecimal maxPrice,

        @Parameter(description = "Minimum context window (tokens)")
        @RequestParam(required = false) Long minContextWindow
    ) {
        List<LLMLeaderboardEntryResponse> leaderboard = llmRankingService.getLeaderboard(
            benchmarkType, provider, creatorSlug, license, maxPrice, minContextWindow
        );
        return ResponseEntity.ok(leaderboard);
    }

    @Operation(
        summary = "Get all benchmarks",
        description = "Retrieve metadata about all 18 benchmarks including descriptions and groupings"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved benchmarks",
            content = @Content(schema = @Schema(implementation = LLMBenchmarkResponse.class))
        )
    })
    @GetMapping("/benchmarks")
    public ResponseEntity<List<LLMBenchmarkResponse>> getAllBenchmarks() {
        List<LLMBenchmarkResponse> benchmarks = llmRankingService.getAllBenchmarks();
        return ResponseEntity.ok(benchmarks);
    }

    @Operation(
        summary = "Get benchmarks by group",
        description = "Retrieve benchmarks filtered by group (Agentic, Reasoning, Coding, Specialized, Composite)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved benchmarks",
            content = @Content(schema = @Schema(implementation = LLMBenchmarkResponse.class))
        )
    })
    @GetMapping("/benchmarks/{categoryGroup}")
    public ResponseEntity<List<LLMBenchmarkResponse>> getBenchmarksByGroup(
        @Parameter(description = "Category group", example = "Agentic")
        @PathVariable String categoryGroup
    ) {
        List<LLMBenchmarkResponse> benchmarks = llmRankingService.getBenchmarksByGroup(categoryGroup);
        return ResponseEntity.ok(benchmarks);
    }
}
