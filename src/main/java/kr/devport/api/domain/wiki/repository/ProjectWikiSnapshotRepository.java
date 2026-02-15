package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.ProjectWikiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for wiki snapshot persistence and retrieval.
 * Supports lookup by project external ID for API reads.
 */
@Repository
public interface ProjectWikiSnapshotRepository extends JpaRepository<ProjectWikiSnapshot, Long> {

    /**
     * Find wiki snapshot by project external ID.
     *
     * @param projectExternalId GitHub project external ID (e.g., "github:12345")
     * @return Wiki snapshot if found and data-ready
     */
    Optional<ProjectWikiSnapshot> findByProjectExternalId(String projectExternalId);

    /**
     * Find data-ready wiki snapshot by project external ID.
     * Used by API to filter out incomplete snapshots.
     *
     * @param projectExternalId GitHub project external ID
     * @param isDataReady Readiness filter (should be true for public API reads)
     * @return Wiki snapshot if found and matches readiness criteria
     */
    Optional<ProjectWikiSnapshot> findByProjectExternalIdAndIsDataReady(
            String projectExternalId,
            boolean isDataReady
    );
}
