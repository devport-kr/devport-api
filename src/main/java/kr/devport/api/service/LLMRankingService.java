package kr.devport.api.service;

import kr.devport.api.domain.entity.Benchmark;
import kr.devport.api.domain.entity.LLMBenchmarkScore;
import kr.devport.api.domain.enums.BenchmarkType;
import kr.devport.api.dto.response.BenchmarkResponse;
import kr.devport.api.dto.response.LLMModelResponse;
import kr.devport.api.dto.response.LLMRankingResponse;
import kr.devport.api.repository.BenchmarkRepository;
import kr.devport.api.repository.LLMBenchmarkScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LLMRankingService {

    private final LLMBenchmarkScoreRepository benchmarkScoreRepository;
    private final BenchmarkRepository benchmarkRepository;

    /**
     * Get LLM rankings for a specific benchmark
     *
     * @param benchmarkType Benchmark type (defaults to AGENTIC_CODING)
     * @param limit Number of models to return
     * @return LLM ranking response with benchmark info and model scores
     */
    @Cacheable(
        value = "llmRankings",
        key = "#benchmarkType != null ? #benchmarkType.name() + '_' + #limit : 'AGENTIC_CODING_' + #limit"
    )
    public LLMRankingResponse getLLMRankings(BenchmarkType benchmarkType, int limit) {
        // Default to AGENTIC_CODING if null
        BenchmarkType type = (benchmarkType != null) ? benchmarkType : BenchmarkType.AGENTIC_CODING;

        // Get benchmark metadata
        Benchmark benchmark = benchmarkRepository.findById(type)
            .orElseThrow(() -> new RuntimeException("Benchmark not found: " + type));

        // Get top N models for this benchmark
        Pageable pageable = PageRequest.of(0, limit);
        List<LLMBenchmarkScore> scores = benchmarkScoreRepository.findByBenchmarkTypeOrderByRankAsc(type, pageable);

        return LLMRankingResponse.builder()
            .benchmark(convertToBenchmarkResponse(benchmark))
            .models(scores.stream()
                .map(this::convertToLLMModelResponse)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Get all benchmark configurations
     *
     * @return List of all benchmarks
     */
    @Cacheable(value = "benchmarks", key = "'all'")
    public List<BenchmarkResponse> getAllBenchmarks() {
        return benchmarkRepository.findAll().stream()
            .map(this::convertToBenchmarkResponse)
            .collect(Collectors.toList());
    }

    /**
     * Convert Benchmark entity to BenchmarkResponse DTO
     */
    private BenchmarkResponse convertToBenchmarkResponse(Benchmark benchmark) {
        return BenchmarkResponse.builder()
            .type(benchmark.getType())
            .labelEn(benchmark.getLabelEn())
            .labelKo(benchmark.getLabelKo())
            .descriptionEn(benchmark.getDescriptionEn())
            .descriptionKo(benchmark.getDescriptionKo())
            .icon(benchmark.getIcon())
            .build();
    }

    /**
     * Convert LLMBenchmarkScore entity to LLMModelResponse DTO
     */
    private LLMModelResponse convertToLLMModelResponse(LLMBenchmarkScore score) {
        return LLMModelResponse.builder()
            .id(score.getModel().getId())
            .name(score.getModel().getName())
            .provider(score.getModel().getProvider())
            .score(score.getScore())
            .rank(score.getRank())
            .contextWindow(score.getModel().getContextWindow())
            .pricing(score.getModel().getPricing())
            .build();
    }
}
