package kr.devport.api.repository;

import kr.devport.api.domain.entity.LLMModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LLMModelRepository extends JpaRepository<LLMModel, Long> {

    Optional<LLMModel> findByModelId(String modelId);

    Optional<LLMModel> findBySlug(String slug);

    Optional<LLMModel> findByExternalId(String externalId);

    List<LLMModel> findByProvider(String provider);

    List<LLMModel> findByModelCreator_Slug(String creatorSlug);

    List<LLMModel> findByLicense(String license);

    @Query("""
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
        ORDER BY m.scoreAaIntelligenceIndex DESC NULLS LAST
    """)
    Page<LLMModel> findWithFilters(
        @Param("provider") String provider,
        @Param("creatorSlug") String creatorSlug,
        @Param("license") String license,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minContextWindow") Long minContextWindow,
        Pageable pageable
    );

    @Query("""
        SELECT m FROM LLMModel m
        LEFT JOIN m.modelCreator mc
        WHERE (:provider IS NULL OR m.provider = :provider)
          AND (:creatorSlug IS NULL OR mc.slug = :creatorSlug)
          AND (:license IS NULL OR m.license = :license)
          AND (:maxPrice IS NULL OR m.priceBlended <= :maxPrice)
          AND (:minContextWindow IS NULL OR m.contextWindow >= :minContextWindow)
    """)
    List<LLMModel> findAllWithFilters(
        @Param("provider") String provider,
        @Param("creatorSlug") String creatorSlug,
        @Param("license") String license,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minContextWindow") Long minContextWindow
    );

    @Query("SELECT m FROM LLMModel m ORDER BY m.scoreAaIntelligenceIndex DESC NULLS LAST")
    List<LLMModel> findAllByOrderByScoreAaIntelligenceIndexDesc();
}
