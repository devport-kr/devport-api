package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.ProjectEvent;
import kr.devport.api.domain.port.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectEventRepository extends JpaRepository<ProjectEvent, Long> {
    @EntityGraph(attributePaths = "project")
    Page<ProjectEvent> findByProject_ExternalId(String projectExternalId, Pageable pageable);

    @EntityGraph(attributePaths = "project")
    @Query("SELECT e FROM ProjectEvent e WHERE e.project.externalId = :projectId " +
           "AND :eventType MEMBER OF e.eventTypes")
    Page<ProjectEvent> findByProjectAndEventType(
        @Param("projectId") String projectExternalId,
        @Param("eventType") EventType eventType,
        Pageable pageable
    );

    List<ProjectEvent> findTop10ByImpactScoreGreaterThanEqualOrderByReleasedAtDesc(
        Integer impactThreshold
    );
}
