package kr.devport.api.llm.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kr.devport.api.llm.enums.BenchmarkType
import java.math.BigDecimal
import java.time.LocalDate

data class LLMModelSearchCondition(
    val provider: String? = null,
    val creatorSlug: String? = null,
    val license: String? = null,
    val maxPrice: BigDecimal? = null,
    val minContextWindow: Long? = null,
    val keyword: String? = null,
    val releaseDateFrom: LocalDate? = null,
    val releaseDateTo: LocalDate? = null,
    val minScore: BigDecimal? = null,
)

data class LLMModelCreateRequest(
    val externalId: String? = null,
    val slug: String? = null,
    @field:NotBlank(message = "Model ID is required")
    val modelId: String = "",
    @field:NotBlank(message = "Model name is required")
    val modelName: String = "",
    val releaseDate: LocalDate? = null,
    val provider: String? = null,
    val modelCreatorId: Long? = null,
    val description: String? = null,
    val priceInput: BigDecimal? = null,
    val priceOutput: BigDecimal? = null,
    val priceBlended: BigDecimal? = null,
    val contextWindow: Long? = null,
    val outputSpeedMedian: BigDecimal? = null,
    val latencyTtft: BigDecimal? = null,
    val medianTimeToFirstAnswerToken: BigDecimal? = null,
    val license: String? = null,
    val scoreTerminalBenchHard: BigDecimal? = null,
    val scoreTauBenchTelecom: BigDecimal? = null,
    val scoreAaLcr: BigDecimal? = null,
    val scoreHumanitysLastExam: BigDecimal? = null,
    val scoreMmluPro: BigDecimal? = null,
    val scoreGpqaDiamond: BigDecimal? = null,
    val scoreLivecodeBench: BigDecimal? = null,
    val scoreScicode: BigDecimal? = null,
    val scoreIfbench: BigDecimal? = null,
    val scoreMath500: BigDecimal? = null,
    val scoreAime: BigDecimal? = null,
    val scoreAime2025: BigDecimal? = null,
    val scoreAaIntelligenceIndex: BigDecimal? = null,
    val scoreAaCodingIndex: BigDecimal? = null,
    val scoreAaMathIndex: BigDecimal? = null,
)

data class LLMModelUpdateRequest(
    val externalId: String? = null,
    val slug: String? = null,
    val modelId: String? = null,
    val modelName: String? = null,
    val releaseDate: LocalDate? = null,
    val provider: String? = null,
    val modelCreatorId: Long? = null,
    val description: String? = null,
    val priceInput: BigDecimal? = null,
    val priceOutput: BigDecimal? = null,
    val priceBlended: BigDecimal? = null,
    val contextWindow: Long? = null,
    val outputSpeedMedian: BigDecimal? = null,
    val latencyTtft: BigDecimal? = null,
    val medianTimeToFirstAnswerToken: BigDecimal? = null,
    val license: String? = null,
    val scoreTerminalBenchHard: BigDecimal? = null,
    val scoreTauBenchTelecom: BigDecimal? = null,
    val scoreAaLcr: BigDecimal? = null,
    val scoreHumanitysLastExam: BigDecimal? = null,
    val scoreMmluPro: BigDecimal? = null,
    val scoreGpqaDiamond: BigDecimal? = null,
    val scoreLivecodeBench: BigDecimal? = null,
    val scoreScicode: BigDecimal? = null,
    val scoreIfbench: BigDecimal? = null,
    val scoreMath500: BigDecimal? = null,
    val scoreAime: BigDecimal? = null,
    val scoreAime2025: BigDecimal? = null,
    val scoreAaIntelligenceIndex: BigDecimal? = null,
    val scoreAaCodingIndex: BigDecimal? = null,
    val scoreAaMathIndex: BigDecimal? = null,
)

data class LLMBenchmarkCreateRequest(
    @field:NotNull(message = "Benchmark type is required")
    val benchmarkType: BenchmarkType? = null,
    @field:NotBlank(message = "Display name is required")
    val displayName: String = "",
    @field:NotBlank(message = "Category group is required")
    val categoryGroup: String = "",
    @field:NotBlank(message = "Description is required")
    val description: String = "",
    val explanation: String? = null,
    @field:NotNull(message = "Sort order is required")
    val sortOrder: Int? = null,
)

data class LLMBenchmarkUpdateRequest(
    val displayName: String? = null,
    val categoryGroup: String? = null,
    val description: String? = null,
    val explanation: String? = null,
    val sortOrder: Int? = null,
)

data class ModelCreatorCreateRequest(
    val externalId: String? = null,
    @field:NotBlank(message = "Slug is required")
    val slug: String = "",
    @field:NotBlank(message = "Name is required")
    val name: String = "",
)

data class ModelCreatorUpdateRequest(
    val externalId: String? = null,
    val slug: String? = null,
    val name: String? = null,
)
