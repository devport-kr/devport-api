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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate

interface ModelCreatorRepository : JpaRepository<ModelCreator, Long> {
    fun findBySlug(slug: String): ModelCreator?

    fun findByExternalId(externalId: String): ModelCreator?

    fun findByName(name: String): ModelCreator?

    fun existsBySlug(slug: String): Boolean
}

interface LLMBenchmarkRepository : JpaRepository<LLMBenchmark, BenchmarkType> {
    fun findAllByOrderBySortOrderAsc(): List<LLMBenchmark>

    fun findByCategoryGroupOrderBySortOrderAsc(categoryGroup: String): List<LLMBenchmark>
}

interface TextToImageModelRepository : JpaRepository<TextToImageModel, Long>

interface TextToVideoModelRepository : JpaRepository<TextToVideoModel, Long>

interface ImageToVideoModelRepository : JpaRepository<ImageToVideoModel, Long>

interface TextToSpeechModelRepository : JpaRepository<TextToSpeechModel, Long>

interface ImageEditingModelRepository : JpaRepository<ImageEditingModel, Long>

interface LLMModelRepository : JpaRepository<LLMModel, Long> {
    fun findByModelId(modelId: String): LLMModel?

    fun findBySlug(slug: String): LLMModel?

    fun findByExternalId(externalId: String): LLMModel?

    @Query(
        """
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
        ORDER BY m.scoreAaIntelligenceIndex DESC NULLS LAST
        """,
        countQuery =
            """
            SELECT COUNT(m) FROM LLMModel m
            LEFT JOIN m.modelCreator mc
            WHERE (:provider IS NULL OR m.provider = :provider)
              AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
              AND (:license IS NULL OR m.license = :license)
              AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
              AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
            """,
    )
    fun findWithFilters(
        @Param("provider") provider: String?,
        @Param("creatorSlug") creatorSlug: String?,
        @Param("license") license: String?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minContextWindow") minContextWindow: Long?,
        pageable: Pageable,
    ): Page<LLMModel>

    @Query(
        """
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
        """,
    )
    fun findAllWithFilters(
        @Param("provider") provider: String?,
        @Param("creatorSlug") creatorSlug: String?,
        @Param("license") license: String?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minContextWindow") minContextWindow: Long?,
    ): List<LLMModel>

    @Query(
        """
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
          AND (:keyword IS NULL OR LOWER(m.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:releaseDateFrom IS NULL OR m.releaseDate >= :releaseDateFrom)
          AND (:releaseDateTo IS NULL OR m.releaseDate <= :releaseDateTo)
          AND (:minScore IS NULL OR m.scoreAaIntelligenceIndex >= :minScore)
        ORDER BY m.scoreAaIntelligenceIndex DESC NULLS LAST
        """,
        countQuery =
            """
            SELECT COUNT(m) FROM LLMModel m
            LEFT JOIN m.modelCreator mc
            WHERE (:provider IS NULL OR m.provider = :provider)
              AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
              AND (:license IS NULL OR m.license = :license)
              AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
              AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
              AND (:keyword IS NULL OR LOWER(m.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:releaseDateFrom IS NULL OR m.releaseDate >= :releaseDateFrom)
              AND (:releaseDateTo IS NULL OR m.releaseDate <= :releaseDateTo)
              AND (:minScore IS NULL OR m.scoreAaIntelligenceIndex >= :minScore)
            """,
    )
    fun findWithExtendedFilters(
        @Param("provider") provider: String?,
        @Param("creatorSlug") creatorSlug: String?,
        @Param("license") license: String?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minContextWindow") minContextWindow: Long?,
        @Param("keyword") keyword: String?,
        @Param("releaseDateFrom") releaseDateFrom: LocalDate?,
        @Param("releaseDateTo") releaseDateTo: LocalDate?,
        @Param("minScore") minScore: BigDecimal?,
        pageable: Pageable,
    ): Page<LLMModel>

    @Query(
        """
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
          AND (:keyword IS NULL OR LOWER(m.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:releaseDateFrom IS NULL OR m.releaseDate >= :releaseDateFrom)
          AND (:releaseDateTo IS NULL OR m.releaseDate <= :releaseDateTo)
          AND (:minScore IS NULL OR m.scoreAaIntelligenceIndex >= :minScore)
        ORDER BY m.scoreAaIntelligenceIndex DESC NULLS LAST
        """,
    )
    fun findAllWithExtendedFilters(
        @Param("provider") provider: String?,
        @Param("creatorSlug") creatorSlug: String?,
        @Param("license") license: String?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minContextWindow") minContextWindow: Long?,
        @Param("keyword") keyword: String?,
        @Param("releaseDateFrom") releaseDateFrom: LocalDate?,
        @Param("releaseDateTo") releaseDateTo: LocalDate?,
        @Param("minScore") minScore: BigDecimal?,
    ): List<LLMModel>
}
