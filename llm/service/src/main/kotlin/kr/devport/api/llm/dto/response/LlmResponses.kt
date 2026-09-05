package kr.devport.api.llm.dto.response

import kr.devport.api.llm.entity.LLMBenchmark
import kr.devport.api.llm.entity.LLMModel
import kr.devport.api.llm.entity.ModelCreator
import kr.devport.api.llm.enums.BenchmarkType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ModelCreatorResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val name: String?,
) {
    companion object {
        fun from(modelCreator: ModelCreator?): ModelCreatorResponse? =
            modelCreator?.let { ModelCreatorResponse(it.id, it.externalId, it.slug, it.name) }
    }
}

data class LLMModelSummaryResponse(
    val id: Long?,
    val slug: String?,
    val modelId: String?,
    val modelName: String?,
    val releaseDate: LocalDate?,
    val provider: String?,
    val modelCreator: ModelCreatorResponse?,
    val scoreAaIntelligenceIndex: BigDecimal?,
    val priceBlended: BigDecimal?,
    val contextWindow: Long?,
    val outputSpeedMedian: BigDecimal?,
    val license: String?,
    val rank: Int?,
) {
    companion object {
        fun fromEntity(
            model: LLMModel,
            rank: Int? = null,
        ): LLMModelSummaryResponse =
            LLMModelSummaryResponse(
                id = model.id,
                slug = model.slug,
                modelId = model.modelId,
                modelName = model.modelName,
                releaseDate = model.releaseDate,
                provider = model.provider,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                scoreAaIntelligenceIndex = model.scoreAaIntelligenceIndex,
                priceBlended = model.priceBlended,
                contextWindow = model.contextWindow,
                outputSpeedMedian = model.outputSpeedMedian,
                license = model.license,
                rank = rank,
            )
    }
}

data class LLMModelDetailResponse(
    val id: Long?,
    val externalId: String?,
    val slug: String?,
    val modelId: String?,
    val modelName: String?,
    val releaseDate: LocalDate?,
    val provider: String?,
    val modelCreator: ModelCreatorResponse?,
    val description: String?,
    val priceInput: BigDecimal?,
    val priceOutput: BigDecimal?,
    val priceBlended: BigDecimal?,
    val contextWindow: Long?,
    val outputSpeedMedian: BigDecimal?,
    val latencyTtft: BigDecimal?,
    val medianTimeToFirstAnswerToken: BigDecimal?,
    val license: String?,
    val scoreTerminalBenchHard: BigDecimal?,
    val scoreTauBenchTelecom: BigDecimal?,
    val scoreAaLcr: BigDecimal?,
    val scoreHumanitysLastExam: BigDecimal?,
    val scoreMmluPro: BigDecimal?,
    val scoreGpqaDiamond: BigDecimal?,
    val scoreLivecodeBench: BigDecimal?,
    val scoreScicode: BigDecimal?,
    val scoreIfbench: BigDecimal?,
    val scoreMath500: BigDecimal?,
    val scoreAime: BigDecimal?,
    val scoreAime2025: BigDecimal?,
    val scoreAaIntelligenceIndex: BigDecimal?,
    val scoreAaCodingIndex: BigDecimal?,
    val scoreAaMathIndex: BigDecimal?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun fromEntity(model: LLMModel): LLMModelDetailResponse =
            LLMModelDetailResponse(
                id = model.id,
                externalId = model.externalId,
                slug = model.slug,
                modelId = model.modelId,
                modelName = model.modelName,
                releaseDate = model.releaseDate,
                provider = model.provider,
                modelCreator = ModelCreatorResponse.from(model.modelCreator),
                description = model.description,
                priceInput = model.priceInput,
                priceOutput = model.priceOutput,
                priceBlended = model.priceBlended,
                contextWindow = model.contextWindow,
                outputSpeedMedian = model.outputSpeedMedian,
                latencyTtft = model.latencyTtft,
                medianTimeToFirstAnswerToken = model.medianTimeToFirstAnswerToken,
                license = model.license,
                scoreTerminalBenchHard = model.scoreTerminalBenchHard,
                scoreTauBenchTelecom = model.scoreTauBenchTelecom,
                scoreAaLcr = model.scoreAaLcr,
                scoreHumanitysLastExam = model.scoreHumanitysLastExam,
                scoreMmluPro = model.scoreMmluPro,
                scoreGpqaDiamond = model.scoreGpqaDiamond,
                scoreLivecodeBench = model.scoreLivecodeBench,
                scoreScicode = model.scoreScicode,
                scoreIfbench = model.scoreIfbench,
                scoreMath500 = model.scoreMath500,
                scoreAime = model.scoreAime,
                scoreAime2025 = model.scoreAime2025,
                scoreAaIntelligenceIndex = model.scoreAaIntelligenceIndex,
                scoreAaCodingIndex = model.scoreAaCodingIndex,
                scoreAaMathIndex = model.scoreAaMathIndex,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
            )
    }
}

data class LLMBenchmarkResponse(
    val benchmarkType: BenchmarkType?,
    val displayName: String?,
    val categoryGroup: String?,
    val description: String?,
    val explanation: String?,
    val sortOrder: Int?,
) {
    companion object {
        fun fromEntity(benchmark: LLMBenchmark): LLMBenchmarkResponse =
            LLMBenchmarkResponse(
                benchmarkType = benchmark.benchmarkType,
                displayName = benchmark.displayName,
                categoryGroup = benchmark.categoryGroup,
                description = benchmark.description,
                explanation = benchmark.explanation,
                sortOrder = benchmark.sortOrder,
            )
    }
}

data class LLMLeaderboardEntryResponse(
    val id: Long?,
    val modelId: String?,
    val modelName: String?,
    val provider: String?,
    val license: String?,
    val score: BigDecimal?,
    val rank: Int?,
    val priceBlended: BigDecimal?,
    val contextWindow: Long?,
) {
    companion object {
        fun fromEntity(
            model: LLMModel,
            benchmarkType: BenchmarkType,
            score: BigDecimal?,
            rank: Int?,
        ): LLMLeaderboardEntryResponse =
            LLMLeaderboardEntryResponse(
                id = model.id,
                modelId = model.modelId,
                modelName = model.modelName,
                provider = model.provider,
                license = model.license,
                score = score,
                rank = rank,
                priceBlended = model.priceBlended,
                contextWindow = model.contextWindow,
            )
    }
}
