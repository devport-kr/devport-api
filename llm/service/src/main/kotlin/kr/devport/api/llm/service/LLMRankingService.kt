package kr.devport.api.llm.service

import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.api.llm.dto.request.LLMModelSearchCondition
import kr.devport.api.llm.dto.response.LLMBenchmarkResponse
import kr.devport.api.llm.dto.response.LLMLeaderboardEntryResponse
import kr.devport.api.llm.dto.response.LLMModelDetailResponse
import kr.devport.api.llm.dto.response.LLMModelSummaryResponse
import kr.devport.api.llm.entity.LLMModel
import kr.devport.api.llm.enums.BenchmarkType
import kr.devport.api.llm.infrastructure.LLMBenchmarkRepository
import kr.devport.api.llm.infrastructure.LLMModelRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class LLMRankingService(
    private val modelRepository: LLMModelRepository,
    private val benchmarkRepository: LLMBenchmarkRepository,
) {
    fun getAllModels(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        pageable: Pageable,
    ): Page<LLMModelSummaryResponse> =
        modelRepository
            .findWithFilters(provider, creatorSlug, license, maxPrice, minContextWindow, unsorted(pageable))
            .map { LLMModelSummaryResponse.fromEntity(it) }

    fun searchModels(
        condition: LLMModelSearchCondition,
        pageable: Pageable,
    ): Page<LLMModelSummaryResponse> =
        modelRepository
            .findWithExtendedFilters(
                condition.provider,
                condition.creatorSlug,
                condition.license,
                condition.maxPrice,
                condition.minContextWindow,
                condition.keyword,
                condition.releaseDateFrom,
                condition.releaseDateTo,
                condition.minScore,
                unsorted(pageable),
            ).map { LLMModelSummaryResponse.fromEntity(it) }

    fun getLeaderboardWithCondition(
        benchmarkType: BenchmarkType,
        condition: LLMModelSearchCondition,
    ): List<LLMLeaderboardEntryResponse> {
        val models =
            modelRepository.findAllWithExtendedFilters(
                condition.provider,
                condition.creatorSlug,
                condition.license,
                condition.maxPrice,
                condition.minContextWindow,
                condition.keyword,
                condition.releaseDateFrom,
                condition.releaseDateTo,
                condition.minScore,
            )
        return calculateRanksForLeaderboard(sortByBenchmark(models, benchmarkType), benchmarkType)
    }

    fun getModelById(modelId: String): LLMModelDetailResponse {
        val model = modelRepository.findByModelId(modelId) ?: throw RuntimeException("Model not found: $modelId")
        return LLMModelDetailResponse.fromEntity(model)
    }

    @Cacheable(
        cacheNames = [CacheNames.LLM_LEADERBOARD],
        key = "@cacheKeyFactory.llmLeaderboardKey(#benchmarkType?.name(), #provider, #creatorSlug, #license, #maxPrice, #minContextWindow)",
        unless = "@cacheFallbackBypass.shouldBypass('LLM')",
    )
    fun getLeaderboard(
        benchmarkType: BenchmarkType,
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
    ): List<LLMLeaderboardEntryResponse> {
        val models = modelRepository.findAllWithFilters(provider, creatorSlug, license, maxPrice, minContextWindow)
        return calculateRanksForLeaderboard(sortByBenchmark(models, benchmarkType), benchmarkType)
    }

    @Cacheable(
        cacheNames = [CacheNames.LLM_BENCHMARKS],
        key = "@cacheKeyFactory.allBenchmarksKey()",
        unless = "@cacheFallbackBypass.shouldBypass('LLM')",
    )
    fun getAllBenchmarks(): List<LLMBenchmarkResponse> =
        benchmarkRepository.findAllByOrderBySortOrderAsc().map { LLMBenchmarkResponse.fromEntity(it) }

    fun getBenchmarksByGroup(categoryGroup: String): List<LLMBenchmarkResponse> =
        benchmarkRepository.findByCategoryGroupOrderBySortOrderAsc(categoryGroup).map { LLMBenchmarkResponse.fromEntity(it) }

    private fun unsorted(pageable: Pageable): Pageable = PageRequest.of(pageable.pageNumber, pageable.pageSize)

    private fun sortByBenchmark(
        models: List<LLMModel>,
        benchmarkType: BenchmarkType,
    ): List<LLMModel> {
        val comparator =
            Comparator
                .comparing({ m: LLMModel -> getScoreForBenchmark(m, benchmarkType) }, nullsLast(naturalOrder<BigDecimal>()))
                .reversed()
        return models.sortedWith(comparator)
    }

    private fun calculateRanksForLeaderboard(
        models: List<LLMModel>,
        benchmarkType: BenchmarkType,
    ): List<LLMLeaderboardEntryResponse> {
        val entries = mutableListOf<LLMLeaderboardEntryResponse>()
        var currentRank = 1
        var previousScore: BigDecimal? = null

        for (model in models) {
            val score = getScoreForBenchmark(model, benchmarkType) ?: continue
            if (previousScore != null && score < previousScore) {
                currentRank = entries.size + 1
            }
            entries.add(LLMLeaderboardEntryResponse.fromEntity(model, benchmarkType, score, currentRank))
            previousScore = score
        }
        return entries
    }

    private fun getScoreForBenchmark(
        model: LLMModel,
        benchmarkType: BenchmarkType,
    ): BigDecimal? =
        when (benchmarkType) {
            BenchmarkType.TERMINAL_BENCH_HARD -> model.scoreTerminalBenchHard
            BenchmarkType.TAU_BENCH_TELECOM -> model.scoreTauBenchTelecom
            BenchmarkType.AA_LCR -> model.scoreAaLcr
            BenchmarkType.HUMANITYS_LAST_EXAM -> model.scoreHumanitysLastExam
            BenchmarkType.MMLU_PRO -> model.scoreMmluPro
            BenchmarkType.GPQA_DIAMOND -> model.scoreGpqaDiamond
            BenchmarkType.LIVECODE_BENCH -> model.scoreLivecodeBench
            BenchmarkType.SCICODE -> model.scoreScicode
            BenchmarkType.IFBENCH -> model.scoreIfbench
            BenchmarkType.MATH_500 -> model.scoreMath500
            BenchmarkType.AIME -> model.scoreAime
            BenchmarkType.AIME_2025 -> model.scoreAime2025
            BenchmarkType.AA_INTELLIGENCE_INDEX -> model.scoreAaIntelligenceIndex
            BenchmarkType.AA_CODING_INDEX -> model.scoreAaCodingIndex
            BenchmarkType.AA_MATH_INDEX -> model.scoreAaMathIndex
        }
}
