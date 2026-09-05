package kr.devport.api.llm.infrastructure

import kr.devport.api.llm.entity.ImageEditingModel
import kr.devport.api.llm.entity.ImageToVideoModel
import kr.devport.api.llm.entity.LLMBenchmark
import kr.devport.api.llm.entity.LLMModel
import kr.devport.api.llm.entity.ModelCreator
import kr.devport.api.llm.entity.TextToImageModel
import kr.devport.api.llm.entity.TextToSpeechModel
import kr.devport.api.llm.entity.TextToVideoModel
import kr.devport.api.llm.enums.BenchmarkType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Out-ports: the persistence contracts owned by the llm core. The Spring Data adapters in
 * :llm:repository-jpa implement these; the core never sees JpaRepository or the repository module.
 */
interface LLMModelRepository {
    fun findByModelId(modelId: String): LLMModel?

    fun findById(id: Long): LLMModel?

    fun save(model: LLMModel): LLMModel

    fun existsById(id: Long): Boolean

    fun deleteById(id: Long)

    fun findWithFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        pageable: Pageable,
    ): Page<LLMModel>

    fun findAllWithFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
    ): List<LLMModel>

    fun findWithExtendedFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        keyword: String?,
        releaseDateFrom: LocalDate?,
        releaseDateTo: LocalDate?,
        minScore: BigDecimal?,
        pageable: Pageable,
    ): Page<LLMModel>

    fun findAllWithExtendedFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        keyword: String?,
        releaseDateFrom: LocalDate?,
        releaseDateTo: LocalDate?,
        minScore: BigDecimal?,
    ): List<LLMModel>
}

interface LLMBenchmarkRepository {
    fun findById(benchmarkType: BenchmarkType): LLMBenchmark?

    fun save(benchmark: LLMBenchmark): LLMBenchmark

    fun existsById(benchmarkType: BenchmarkType): Boolean

    fun deleteById(benchmarkType: BenchmarkType)

    fun findAllByOrderBySortOrderAsc(): List<LLMBenchmark>

    fun findByCategoryGroupOrderBySortOrderAsc(categoryGroup: String): List<LLMBenchmark>
}

interface ModelCreatorRepository {
    fun findById(id: Long): ModelCreator?

    fun save(creator: ModelCreator): ModelCreator

    fun existsById(id: Long): Boolean

    fun deleteById(id: Long)
}

interface TextToImageModelRepository {
    fun findAll(pageable: Pageable): Page<TextToImageModel>
}

interface TextToVideoModelRepository {
    fun findAll(pageable: Pageable): Page<TextToVideoModel>
}

interface ImageToVideoModelRepository {
    fun findAll(pageable: Pageable): Page<ImageToVideoModel>
}

interface TextToSpeechModelRepository {
    fun findAll(pageable: Pageable): Page<TextToSpeechModel>
}

interface ImageEditingModelRepository {
    fun findAll(pageable: Pageable): Page<ImageEditingModel>
}
