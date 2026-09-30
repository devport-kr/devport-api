package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.ModelCreator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ModelCreator entity (read-only operations).
 * This application only reads data; writing is done by external services.
 */
@Repository
public interface ModelCreatorRepository extends JpaRepository<ModelCreator, Long> {

    /**
     * Find model creator by slug.
     * @param slug URL-friendly identifier (e.g., "openai", "anthropic")
     * @return Optional ModelCreator
     */
    Optional<ModelCreator> findBySlug(String slug);

    /**
     * Find model creator by external ID from API.
     * @param externalId UUID from Artificial Analysis API
     * @return Optional ModelCreator
     */
    Optional<ModelCreator> findByExternalId(String externalId);

    /**
     * Find model creator by name.
     * @param name Display name (e.g., "OpenAI", "Anthropic")
     * @return Optional ModelCreator
     */
    Optional<ModelCreator> findByName(String name);

    /**
     * Check if a creator exists by slug.
     * @param slug URL-friendly identifier
     * @return true if exists
     */
    boolean existsBySlug(String slug);
}
