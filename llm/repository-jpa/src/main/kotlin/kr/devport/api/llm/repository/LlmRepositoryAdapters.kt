package kr.devport.api.llm.repository

import kr.devport.api.llm.entity.ImageEditingModel
import kr.devport.api.llm.entity.ImageToVideoModel
import kr.devport.api.llm.entity.LLMBenchmark
import kr.devport.api.llm.entity.LLMModel
import kr.devport.api.llm.entity.ModelCreator
import kr.devport.api.llm.entity.TextToImageModel
import kr.devport.api.llm.entity.TextToSpeechModel
import kr.devport.api.llm.entity.TextToVideoModel
import kr.devport.api.llm.enums.BenchmarkType
import kr.devport.api.llm.infrastructure.ImageEditingModelRepository
import kr.devport.api.llm.infrastructure.ImageToVideoModelRepository
import kr.devport.api.llm.infrastructure.LLMBenchmarkRepository
import kr.devport.api.llm.infrastructure.LLMModelRepository
import kr.devport.api.llm.infrastructure.ModelCreatorRepository
import kr.devport.api.llm.infrastructure.TextToImageModelRepository
import kr.devport.api.llm.infrastructure.TextToSpeechModelRepository
import kr.devport.api.llm.infrastructure.TextToVideoModelRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

/** Out-port adapters: bridge the core ports to the Spring Data interfaces. */
@Repository
class LLMModelRepositoryAdapter(
    private val jpa: LLMModelJpaRepository,
) : LLMModelRepository {
    override fun findByModelId(modelId: String): LLMModel? = jpa.findByModelId(modelId)

    override fun findById(id: Long): LLMModel? = jpa.findById(id).orElse(null)

    override fun save(model: LLMModel): LLMModel = jpa.save(model)

    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun findWithFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        pageable: Pageable,
    ): Page<LLMModel> = jpa.findWithFilters(provider, creatorSlug, license, maxPrice, minContextWindow, pageable)

    override fun findAllWithFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
    ): List<LLMModel> = jpa.findAllWithFilters(provider, creatorSlug, license, maxPrice, minContextWindow)

    override fun findWithExtendedFilters(
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
    ): Page<LLMModel> =
        jpa.findWithExtendedFilters(
            provider, creatorSlug, license, maxPrice, minContextWindow,
            keyword, releaseDateFrom, releaseDateTo, minScore, pageable,
        )

    override fun findAllWithExtendedFilters(
        provider: String?,
        creatorSlug: String?,
        license: String?,
        maxPrice: BigDecimal?,
        minContextWindow: Long?,
        keyword: String?,
        releaseDateFrom: LocalDate?,
        releaseDateTo: LocalDate?,
        minScore: BigDecimal?,
    ): List<LLMModel> =
        jpa.findAllWithExtendedFilters(
            provider, creatorSlug, license, maxPrice, minContextWindow,
            keyword, releaseDateFrom, releaseDateTo, minScore,
        )
}

@Repository
class LLMBenchmarkRepositoryAdapter(
    private val jpa: LLMBenchmarkJpaRepository,
) : LLMBenchmarkRepository {
    override fun findById(benchmarkType: BenchmarkType): LLMBenchmark? = jpa.findById(benchmarkType).orElse(null)

    override fun save(benchmark: LLMBenchmark): LLMBenchmark = jpa.save(benchmark)

    override fun existsById(benchmarkType: BenchmarkType): Boolean = jpa.existsById(benchmarkType)

    override fun deleteById(benchmarkType: BenchmarkType) = jpa.deleteById(benchmarkType)

    override fun findAllByOrderBySortOrderAsc(): List<LLMBenchmark> = jpa.findAllByOrderBySortOrderAsc()

    override fun findByCategoryGroupOrderBySortOrderAsc(categoryGroup: String): List<LLMBenchmark> =
        jpa.findByCategoryGroupOrderBySortOrderAsc(categoryGroup)
}

@Repository
class ModelCreatorRepositoryAdapter(
    private val jpa: ModelCreatorJpaRepository,
) : ModelCreatorRepository {
    override fun findById(id: Long): ModelCreator? = jpa.findById(id).orElse(null)

    override fun save(creator: ModelCreator): ModelCreator = jpa.save(creator)

    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    override fun deleteById(id: Long) = jpa.deleteById(id)
}

@Repository
class TextToImageModelRepositoryAdapter(
    private val jpa: TextToImageModelJpaRepository,
) : TextToImageModelRepository {
    override fun findAll(pageable: Pageable): Page<TextToImageModel> = jpa.findAll(pageable)
}

@Repository
class TextToVideoModelRepositoryAdapter(
    private val jpa: TextToVideoModelJpaRepository,
) : TextToVideoModelRepository {
    override fun findAll(pageable: Pageable): Page<TextToVideoModel> = jpa.findAll(pageable)
}

@Repository
class ImageToVideoModelRepositoryAdapter(
    private val jpa: ImageToVideoModelJpaRepository,
) : ImageToVideoModelRepository {
    override fun findAll(pageable: Pageable): Page<ImageToVideoModel> = jpa.findAll(pageable)
}

@Repository
class TextToSpeechModelRepositoryAdapter(
    private val jpa: TextToSpeechModelJpaRepository,
) : TextToSpeechModelRepository {
    override fun findAll(pageable: Pageable): Page<TextToSpeechModel> = jpa.findAll(pageable)
}

@Repository
class ImageEditingModelRepositoryAdapter(
    private val jpa: ImageEditingModelJpaRepository,
) : ImageEditingModelRepository {
    override fun findAll(pageable: Pageable): Page<ImageEditingModel> = jpa.findAll(pageable)
}
