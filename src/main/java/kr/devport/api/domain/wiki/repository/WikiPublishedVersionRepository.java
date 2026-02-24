package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiPublishedVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiPublishedVersionRepository extends JpaRepository<WikiPublishedVersion, Long> {

    Optional<WikiPublishedVersion> findTopByProjectIdOrderByVersionNumberDesc(Long projectId);

    List<WikiPublishedVersion> findByProjectIdOrderByVersionNumberDesc(Long projectId);

    Optional<WikiPublishedVersion> findByProjectIdAndVersionNumber(Long projectId, Integer versionNumber);

    boolean existsByProjectIdAndVersionNumber(Long projectId, Integer versionNumber);
}
