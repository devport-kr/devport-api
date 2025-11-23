package kr.devport.api.controller;

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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LLMRankingController {

    private final LLMRankingService llmRankingService;

    /**
     * GET /api/llm-rankings
     * Get LLM benchmark rankings
     *
     * @param benchmark Benchmark type (default: AGENTIC_CODING)
     *                  Options: AGENTIC_CODING, REASONING, MATH, VISUAL, MULTILINGUAL
     * @param limit Number of models to return (default: 8)
     * @return LLM ranking response with benchmark info and model scores
     */
    @GetMapping("/llm-rankings")
    public ResponseEntity<LLMRankingResponse> getLLMRankings(
        @RequestParam(defaultValue = "AGENTIC_CODING") BenchmarkType benchmark,
        @RequestParam(defaultValue = "8") int limit
    ) {
        LLMRankingResponse response = llmRankingService.getLLMRankings(benchmark, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/benchmarks
     * Get all benchmark configurations
     *
     * @return List of all benchmarks (reference data for frontend)
     */
    @GetMapping("/benchmarks")
    public ResponseEntity<List<BenchmarkResponse>> getAllBenchmarks() {
        List<BenchmarkResponse> response = llmRankingService.getAllBenchmarks();
        return ResponseEntity.ok(response);
    }
}
