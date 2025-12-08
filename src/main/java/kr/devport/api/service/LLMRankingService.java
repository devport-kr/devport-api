package kr.devport.api.service;

import kr.devport.api.domain.entity.LLMBenchmark;
import kr.devport.api.domain.entity.LLMModel;
import kr.devport.api.domain.enums.BenchmarkType;
import kr.devport.api.dto.response.*;
import kr.devport.api.repository.LLMBenchmarkRepository;
import kr.devport.api.repository.LLMModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LLMRankingService {

    private final LLMModelRepository modelRepository;
    private final LLMBenchmarkRepository benchmarkRepository;

    /**
     * Get all models with filters (paginated)
     * Default sort: Intelligence Index DESC
     */
    public Page<LLMModelSummaryResponse> getAllModels(
        String provider,
        String creatorSlug,
        String license,
        BigDecimal maxPrice,
        Long minContextWindow,
        Pageable pageable
    ) {
        Page<LLMModel> models = modelRepository.findWithFilters(
            provider, creatorSlug, license, maxPrice, minContextWindow, pageable
        );

        return models.map(model -> {
            // Calculate rank based on intelligence index
            Integer rank = calculateRankForModel(model, null);
            return LLMModelSummaryResponse.fromEntity(model, rank);
        });
    }

    /**
     * Get a specific model by model ID
     */
    public LLMModelDetailResponse getModelById(String modelId) {
        LLMModel model = modelRepository.findByModelId(modelId)
            .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        return LLMModelDetailResponse.fromEntity(model);
    }

    /**
     * Get leaderboard for a specific benchmark
     * Calculates ranks dynamically based on the benchmark's score field
     */
    @Cacheable(
        value = "llmLeaderboard",
        key = "#benchmarkType.name() + '_' + (#provider != null ? #provider : 'all') + '_' + (#creatorSlug != null ? #creatorSlug : 'all') + '_' + (#license != null ? #license : 'all')"
    )
    public List<LLMLeaderboardEntryResponse> getLeaderboard(
        BenchmarkType benchmarkType,
        String provider,
        String creatorSlug,
        String license,
        BigDecimal maxPrice,
        Long minContextWindow
    ) {
        // Get all models with filters
        List<LLMModel> models = modelRepository.findAllWithFilters(
            provider, creatorSlug, license, maxPrice, minContextWindow
        );

        // Sort by the specific benchmark score
        models.sort(getComparatorForBenchmark(benchmarkType).reversed());

        // Calculate ranks and convert to DTOs
        return calculateRanksForLeaderboard(models, benchmarkType);
    }

    /**
     * Get all benchmarks
     */
    @Cacheable(value = "benchmarks", key = "'all'")
    public List<LLMBenchmarkResponse> getAllBenchmarks() {
        return benchmarkRepository.findAllByOrderBySortOrderAsc().stream()
            .map(LLMBenchmarkResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get benchmarks by category group
     */
    public List<LLMBenchmarkResponse> getBenchmarksByGroup(String categoryGroup) {
        return benchmarkRepository.findByCategoryGroupOrderBySortOrderAsc(categoryGroup).stream()
            .map(LLMBenchmarkResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Calculate ranks for leaderboard entries
     * Uses dense ranking (ties get the same rank, no gaps)
     */
    private List<LLMLeaderboardEntryResponse> calculateRanksForLeaderboard(
        List<LLMModel> models,
        BenchmarkType benchmarkType
    ) {
        List<LLMLeaderboardEntryResponse> entries = new ArrayList<>();
        int currentRank = 1;
        BigDecimal previousScore = null;

        for (int i = 0; i < models.size(); i++) {
            LLMModel model = models.get(i);
            BigDecimal score = getScoreForBenchmark(model, benchmarkType);

            // Skip models without a score for this benchmark
            if (score == null) {
                continue;
            }

            // Update rank if score decreased (handle ties)
            if (previousScore != null && score.compareTo(previousScore) < 0) {
                currentRank = entries.size() + 1;
            }

            entries.add(LLMLeaderboardEntryResponse.fromEntity(
                model, benchmarkType, score, currentRank
            ));

            previousScore = score;
        }

        return entries;
    }

    /**
     * Calculate rank for a single model (simplified version)
     * Used for summary responses
     */
    private Integer calculateRankForModel(LLMModel model, BenchmarkType benchmarkType) {
        // For simplicity, just return null - full ranking requires all models
        // Can be implemented if needed for specific use cases
        return null;
    }

    /**
     * Get comparator for sorting by benchmark score
     */
    private Comparator<LLMModel> getComparatorForBenchmark(BenchmarkType benchmarkType) {
        return Comparator.comparing(
            model -> getScoreForBenchmark(model, benchmarkType),
            Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    /**
     * Get score for a specific benchmark from model
     * Uses switch expression to map benchmark type to score field
     */
    private BigDecimal getScoreForBenchmark(LLMModel model, BenchmarkType benchmarkType) {
        return switch (benchmarkType) {
            // Agentic Capabilities
            case TERMINAL_BENCH_HARD -> model.getScoreTerminalBenchHard();
            case TAU_BENCH_TELECOM -> model.getScoreTauBenchTelecom();

            // Reasoning & Knowledge
            case AA_LCR -> model.getScoreAaLcr();
            case HUMANITYS_LAST_EXAM -> model.getScoreHumanitysLastExam();
            case MMLU_PRO -> model.getScoreMmluPro();
            case GPQA_DIAMOND -> model.getScoreGpqaDiamond();

            // Coding
            case LIVECODE_BENCH -> model.getScoreLivecodeBench();
            case SCICODE -> model.getScoreScicode();

            // Specialized Skills
            case IFBENCH -> model.getScoreIfbench();
            case MATH_500 -> model.getScoreMath500();
            case AIME -> model.getScoreAime();
            case AIME_2025 -> model.getScoreAime2025();

            // Composite Indices
            case AA_INTELLIGENCE_INDEX -> model.getScoreAaIntelligenceIndex();
            case AA_CODING_INDEX -> model.getScoreAaCodingIndex();
            case AA_MATH_INDEX -> model.getScoreAaMathIndex();
        };
    }
}
