package kr.devport.api.llm.controller.admin

import jakarta.validation.Valid
import kr.devport.api.llm.dto.request.LLMBenchmarkCreateRequest
import kr.devport.api.llm.dto.request.LLMBenchmarkUpdateRequest
import kr.devport.api.llm.dto.request.LLMModelCreateRequest
import kr.devport.api.llm.dto.request.LLMModelUpdateRequest
import kr.devport.api.llm.dto.request.ModelCreatorCreateRequest
import kr.devport.api.llm.dto.request.ModelCreatorUpdateRequest
import kr.devport.api.llm.dto.response.LLMBenchmarkResponse
import kr.devport.api.llm.dto.response.LLMModelDetailResponse
import kr.devport.api.llm.dto.response.ModelCreatorResponse
import kr.devport.api.llm.enums.BenchmarkType
import kr.devport.api.llm.service.admin.LLMBenchmarkAdminService
import kr.devport.api.llm.service.admin.LLMModelAdminService
import kr.devport.api.llm.service.admin.ModelCreatorAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/llm-models")
class LLMModelAdminController(
    private val llmModelAdminService: LLMModelAdminService,
) {
    @PostMapping
    fun createLLMModel(
        @Valid @RequestBody request: LLMModelCreateRequest,
    ): ResponseEntity<LLMModelDetailResponse> = ResponseEntity.status(HttpStatus.CREATED).body(llmModelAdminService.createLLMModel(request))

    @PutMapping("/{id}")
    fun updateLLMModel(
        @PathVariable id: Long,
        @Valid @RequestBody request: LLMModelUpdateRequest,
    ): ResponseEntity<LLMModelDetailResponse> = ResponseEntity.ok(llmModelAdminService.updateLLMModel(id, request))

    @DeleteMapping("/{id}")
    fun deleteLLMModel(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        llmModelAdminService.deleteLLMModel(id)
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/admin/llm-benchmarks")
class LLMBenchmarkAdminController(
    private val llmBenchmarkAdminService: LLMBenchmarkAdminService,
) {
    @PostMapping
    fun createLLMBenchmark(
        @Valid @RequestBody request: LLMBenchmarkCreateRequest,
    ): ResponseEntity<LLMBenchmarkResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(llmBenchmarkAdminService.createLLMBenchmark(request))

    @PutMapping("/{benchmarkType}")
    fun updateLLMBenchmark(
        @PathVariable benchmarkType: BenchmarkType,
        @Valid @RequestBody request: LLMBenchmarkUpdateRequest,
    ): ResponseEntity<LLMBenchmarkResponse> = ResponseEntity.ok(llmBenchmarkAdminService.updateLLMBenchmark(benchmarkType, request))

    @DeleteMapping("/{benchmarkType}")
    fun deleteLLMBenchmark(
        @PathVariable benchmarkType: BenchmarkType,
    ): ResponseEntity<Void> {
        llmBenchmarkAdminService.deleteLLMBenchmark(benchmarkType)
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/admin/model-creators")
class ModelCreatorAdminController(
    private val modelCreatorAdminService: ModelCreatorAdminService,
) {
    @PostMapping
    fun createModelCreator(
        @Valid @RequestBody request: ModelCreatorCreateRequest,
    ): ResponseEntity<ModelCreatorResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(modelCreatorAdminService.createModelCreator(request))

    @PutMapping("/{id}")
    fun updateModelCreator(
        @PathVariable id: Long,
        @Valid @RequestBody request: ModelCreatorUpdateRequest,
    ): ResponseEntity<ModelCreatorResponse> = ResponseEntity.ok(modelCreatorAdminService.updateModelCreator(id, request))

    @DeleteMapping("/{id}")
    fun deleteModelCreator(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        modelCreatorAdminService.deleteModelCreator(id)
        return ResponseEntity.noContent().build()
    }
}
