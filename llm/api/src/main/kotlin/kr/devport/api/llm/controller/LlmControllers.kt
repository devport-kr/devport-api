package kr.devport.api.llm.controller

import io.swagger.v3.oas.annotations.tags.Tag
import kr.devport.api.llm.dto.request.LLMModelSearchCondition
import kr.devport.api.llm.dto.response.ImageEditingModelResponse
import kr.devport.api.llm.dto.response.ImageToVideoModelResponse
import kr.devport.api.llm.dto.response.LLMBenchmarkResponse
import kr.devport.api.llm.dto.response.LLMLeaderboardEntryResponse
import kr.devport.api.llm.dto.response.LLMModelDetailResponse
import kr.devport.api.llm.dto.response.LLMModelSummaryResponse
import kr.devport.api.llm.dto.response.TextToImageModelResponse
import kr.devport.api.llm.dto.response.TextToSpeechModelResponse
import kr.devport.api.llm.dto.response.TextToVideoModelResponse
import kr.devport.api.llm.enums.BenchmarkType
import kr.devport.api.llm.service.LLMMediaService
import kr.devport.api.llm.service.LLMRankingService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

@Tag(name = "LLM Rankings", description = "LLM benchmark rankings and performance metrics (public)")
@RestController
@RequestMapping("/api/llm")
class LLMRankingController(
    private val llmRankingService: LLMRankingService,
) {
    @GetMapping("/models")
    fun getAllModels(
        @RequestParam(required = false) provider: String?,
        @RequestParam(required = false) creatorSlug: String?,
        @RequestParam(required = false) license: String?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) minContextWindow: Long?,
        @PageableDefault(size = 20, sort = ["scoreAaIntelligenceIndex"]) pageable: Pageable,
    ): ResponseEntity<Page<LLMModelSummaryResponse>> =
        ResponseEntity.ok(llmRankingService.getAllModels(provider, creatorSlug, license, maxPrice, minContextWindow, pageable))

    @GetMapping("/models/{modelId}")
    fun getModelById(
        @PathVariable modelId: String,
    ): ResponseEntity<LLMModelDetailResponse> = ResponseEntity.ok(llmRankingService.getModelById(modelId))

    @GetMapping("/models/search")
    fun searchModels(
        @RequestParam(required = false) provider: String?,
        @RequestParam(required = false) creatorSlug: String?,
        @RequestParam(required = false) license: String?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) minContextWindow: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) releaseDateFrom: LocalDate?,
        @RequestParam(required = false) releaseDateTo: LocalDate?,
        @RequestParam(required = false) minScore: BigDecimal?,
        @PageableDefault(size = 20, sort = ["scoreAaIntelligenceIndex"]) pageable: Pageable,
    ): ResponseEntity<Page<LLMModelSummaryResponse>> {
        val condition =
            LLMModelSearchCondition(
                provider = provider,
                creatorSlug = creatorSlug,
                license = license,
                maxPrice = maxPrice,
                minContextWindow = minContextWindow,
                keyword = keyword,
                releaseDateFrom = releaseDateFrom,
                releaseDateTo = releaseDateTo,
                minScore = minScore,
            )
        return ResponseEntity.ok(llmRankingService.searchModels(condition, pageable))
    }

    @GetMapping("/leaderboard/{benchmarkType}")
    fun getLeaderboard(
        @PathVariable benchmarkType: BenchmarkType,
        @RequestParam(required = false) provider: String?,
        @RequestParam(required = false) creatorSlug: String?,
        @RequestParam(required = false) license: String?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) minContextWindow: Long?,
    ): ResponseEntity<List<LLMLeaderboardEntryResponse>> =
        ResponseEntity.ok(llmRankingService.getLeaderboard(benchmarkType, provider, creatorSlug, license, maxPrice, minContextWindow))

    @GetMapping("/benchmarks")
    fun getAllBenchmarks(): ResponseEntity<List<LLMBenchmarkResponse>> = ResponseEntity.ok(llmRankingService.getAllBenchmarks())

    @GetMapping("/benchmarks/{categoryGroup}")
    fun getBenchmarksByGroup(
        @PathVariable categoryGroup: String,
    ): ResponseEntity<List<LLMBenchmarkResponse>> = ResponseEntity.ok(llmRankingService.getBenchmarksByGroup(categoryGroup))
}

@Tag(name = "LLM Media", description = "Media model benchmarks from Artificial Analysis (public)")
@RestController
@RequestMapping("/api/llm/media")
class LLMMediaController(
    private val llmMediaService: LLMMediaService,
) {
    @GetMapping("/text-to-image")
    fun getTextToImageModels(
        @PageableDefault(size = 20, sort = ["rank"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<TextToImageModelResponse>> = ResponseEntity.ok(llmMediaService.getTextToImageModels(pageable))

    @GetMapping("/image-editing")
    fun getImageEditingModels(
        @PageableDefault(size = 20, sort = ["rank"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<ImageEditingModelResponse>> = ResponseEntity.ok(llmMediaService.getImageEditingModels(pageable))

    @GetMapping("/text-to-speech")
    fun getTextToSpeechModels(
        @PageableDefault(size = 20, sort = ["rank"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<TextToSpeechModelResponse>> = ResponseEntity.ok(llmMediaService.getTextToSpeechModels(pageable))

    @GetMapping("/text-to-video")
    fun getTextToVideoModels(
        @PageableDefault(size = 20, sort = ["rank"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<TextToVideoModelResponse>> = ResponseEntity.ok(llmMediaService.getTextToVideoModels(pageable))

    @GetMapping("/image-to-video")
    fun getImageToVideoModels(
        @PageableDefault(size = 20, sort = ["rank"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<ImageToVideoModelResponse>> = ResponseEntity.ok(llmMediaService.getImageToVideoModels(pageable))
}
