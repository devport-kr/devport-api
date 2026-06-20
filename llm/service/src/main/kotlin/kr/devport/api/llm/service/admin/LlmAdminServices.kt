package kr.devport.api.llm.service.admin

import kr.devport.api.domain.common.cache.CacheNames
import kr.devport.api.llm.dto.request.LLMBenchmarkCreateRequest
import kr.devport.api.llm.dto.request.LLMBenchmarkUpdateRequest
import kr.devport.api.llm.dto.request.LLMModelCreateRequest
import kr.devport.api.llm.dto.request.LLMModelUpdateRequest
import kr.devport.api.llm.dto.request.ModelCreatorCreateRequest
import kr.devport.api.llm.dto.request.ModelCreatorUpdateRequest
import kr.devport.api.llm.dto.response.LLMBenchmarkResponse
import kr.devport.api.llm.dto.response.LLMModelDetailResponse
import kr.devport.api.llm.dto.response.ModelCreatorResponse
import kr.devport.api.llm.entity.LLMBenchmark
import kr.devport.api.llm.entity.LLMModel
import kr.devport.api.llm.entity.ModelCreator
import kr.devport.api.llm.enums.BenchmarkType
import kr.devport.api.llm.repository.LLMBenchmarkRepository
import kr.devport.api.llm.repository.LLMModelRepository
import kr.devport.api.llm.repository.ModelCreatorRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LLMModelAdminService(
    private val llmModelRepository: LLMModelRepository,
    private val modelCreatorRepository: ModelCreatorRepository,
) {
    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun createLLMModel(request: LLMModelCreateRequest): LLMModelDetailResponse {
        val model =
            LLMModel().apply {
                externalId = request.externalId
                slug = request.slug
                modelId = request.modelId
                modelName = request.modelName
                releaseDate = request.releaseDate
                provider = request.provider
                description = request.description
                priceInput = request.priceInput
                priceOutput = request.priceOutput
                priceBlended = request.priceBlended
                contextWindow = request.contextWindow
                outputSpeedMedian = request.outputSpeedMedian
                latencyTtft = request.latencyTtft
                medianTimeToFirstAnswerToken = request.medianTimeToFirstAnswerToken
                license = request.license
                scoreTerminalBenchHard = request.scoreTerminalBenchHard
                scoreTauBenchTelecom = request.scoreTauBenchTelecom
                scoreAaLcr = request.scoreAaLcr
                scoreHumanitysLastExam = request.scoreHumanitysLastExam
                scoreMmluPro = request.scoreMmluPro
                scoreGpqaDiamond = request.scoreGpqaDiamond
                scoreLivecodeBench = request.scoreLivecodeBench
                scoreScicode = request.scoreScicode
                scoreIfbench = request.scoreIfbench
                scoreMath500 = request.scoreMath500
                scoreAime = request.scoreAime
                scoreAime2025 = request.scoreAime2025
                scoreAaIntelligenceIndex = request.scoreAaIntelligenceIndex
                scoreAaCodingIndex = request.scoreAaCodingIndex
                scoreAaMathIndex = request.scoreAaMathIndex
            }
        request.modelCreatorId?.let { model.modelCreator = findCreator(it) }
        return LLMModelDetailResponse.fromEntity(llmModelRepository.save(model))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun updateLLMModel(
        id: Long,
        request: LLMModelUpdateRequest,
    ): LLMModelDetailResponse {
        val model = llmModelRepository.findById(id).orElseThrow { IllegalArgumentException("LLMModel not found with id: $id") }
        request.externalId?.let { model.externalId = it }
        request.slug?.let { model.slug = it }
        request.modelId?.let { model.modelId = it }
        request.modelName?.let { model.modelName = it }
        request.releaseDate?.let { model.releaseDate = it }
        request.provider?.let { model.provider = it }
        request.description?.let { model.description = it }
        request.priceInput?.let { model.priceInput = it }
        request.priceOutput?.let { model.priceOutput = it }
        request.priceBlended?.let { model.priceBlended = it }
        request.contextWindow?.let { model.contextWindow = it }
        request.outputSpeedMedian?.let { model.outputSpeedMedian = it }
        request.latencyTtft?.let { model.latencyTtft = it }
        request.medianTimeToFirstAnswerToken?.let { model.medianTimeToFirstAnswerToken = it }
        request.license?.let { model.license = it }
        request.scoreTerminalBenchHard?.let { model.scoreTerminalBenchHard = it }
        request.scoreTauBenchTelecom?.let { model.scoreTauBenchTelecom = it }
        request.scoreAaLcr?.let { model.scoreAaLcr = it }
        request.scoreHumanitysLastExam?.let { model.scoreHumanitysLastExam = it }
        request.scoreMmluPro?.let { model.scoreMmluPro = it }
        request.scoreGpqaDiamond?.let { model.scoreGpqaDiamond = it }
        request.scoreLivecodeBench?.let { model.scoreLivecodeBench = it }
        request.scoreScicode?.let { model.scoreScicode = it }
        request.scoreIfbench?.let { model.scoreIfbench = it }
        request.scoreMath500?.let { model.scoreMath500 = it }
        request.scoreAime?.let { model.scoreAime = it }
        request.scoreAime2025?.let { model.scoreAime2025 = it }
        request.scoreAaIntelligenceIndex?.let { model.scoreAaIntelligenceIndex = it }
        request.scoreAaCodingIndex?.let { model.scoreAaCodingIndex = it }
        request.scoreAaMathIndex?.let { model.scoreAaMathIndex = it }
        request.modelCreatorId?.let { model.modelCreator = findCreator(it) }
        return LLMModelDetailResponse.fromEntity(llmModelRepository.save(model))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun deleteLLMModel(id: Long) {
        require(llmModelRepository.existsById(id)) { "LLMModel not found with id: $id" }
        llmModelRepository.deleteById(id)
    }

    private fun findCreator(creatorId: Long): ModelCreator =
        modelCreatorRepository
            .findById(creatorId)
            .orElseThrow { IllegalArgumentException("ModelCreator not found with id: $creatorId") }
}

@Service
@Transactional
class LLMBenchmarkAdminService(
    private val llmBenchmarkRepository: LLMBenchmarkRepository,
) {
    @CacheEvict(cacheNames = [CacheNames.LLM_BENCHMARKS], allEntries = true)
    fun createLLMBenchmark(request: LLMBenchmarkCreateRequest): LLMBenchmarkResponse {
        val benchmark =
            LLMBenchmark().apply {
                benchmarkType = request.benchmarkType
                displayName = request.displayName
                categoryGroup = request.categoryGroup
                description = request.description
                explanation = request.explanation
                sortOrder = request.sortOrder
            }
        return LLMBenchmarkResponse.fromEntity(llmBenchmarkRepository.save(benchmark))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_BENCHMARKS], allEntries = true)
    fun updateLLMBenchmark(
        benchmarkType: BenchmarkType,
        request: LLMBenchmarkUpdateRequest,
    ): LLMBenchmarkResponse {
        val benchmark =
            llmBenchmarkRepository
                .findById(benchmarkType)
                .orElseThrow { IllegalArgumentException("LLMBenchmark not found with type: $benchmarkType") }
        request.displayName?.let { benchmark.displayName = it }
        request.categoryGroup?.let { benchmark.categoryGroup = it }
        request.description?.let { benchmark.description = it }
        request.explanation?.let { benchmark.explanation = it }
        request.sortOrder?.let { benchmark.sortOrder = it }
        return LLMBenchmarkResponse.fromEntity(llmBenchmarkRepository.save(benchmark))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_BENCHMARKS], allEntries = true)
    fun deleteLLMBenchmark(benchmarkType: BenchmarkType) {
        require(llmBenchmarkRepository.existsById(benchmarkType)) { "LLMBenchmark not found with type: $benchmarkType" }
        llmBenchmarkRepository.deleteById(benchmarkType)
    }
}

@Service
@Transactional
class ModelCreatorAdminService(
    private val modelCreatorRepository: ModelCreatorRepository,
) {
    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun createModelCreator(request: ModelCreatorCreateRequest): ModelCreatorResponse {
        val creator =
            ModelCreator().apply {
                externalId = request.externalId
                slug = request.slug
                name = request.name
            }
        return toResponse(modelCreatorRepository.save(creator))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun updateModelCreator(
        id: Long,
        request: ModelCreatorUpdateRequest,
    ): ModelCreatorResponse {
        val creator = modelCreatorRepository.findById(id).orElseThrow { IllegalArgumentException("ModelCreator not found with id: $id") }
        request.externalId?.let { creator.externalId = it }
        request.slug?.let { creator.slug = it }
        request.name?.let { creator.name = it }
        return toResponse(modelCreatorRepository.save(creator))
    }

    @CacheEvict(cacheNames = [CacheNames.LLM_MODELS, CacheNames.LLM_LEADERBOARD], allEntries = true)
    fun deleteModelCreator(id: Long) {
        require(modelCreatorRepository.existsById(id)) { "ModelCreator not found with id: $id" }
        modelCreatorRepository.deleteById(id)
    }

    private fun toResponse(creator: ModelCreator): ModelCreatorResponse =
        ModelCreatorResponse(creator.id, creator.externalId, creator.slug, creator.name)
}
