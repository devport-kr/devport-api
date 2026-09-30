package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    Optional<ProjectComment> findByExternalId(String externalId);

    @Query("""
        SELECT c FROM ProjectComment c
        JOIN FETCH c.user
        WHERE c.project.externalId = :projectExternalId
        ORDER BY c.createdAt ASC
    """)
    List<ProjectComment> findAllByProjectExternalId(@Param("projectExternalId") String projectExternalId);

    @Query("SELECT COUNT(c) FROM ProjectComment c WHERE c.project.externalId = :projectExternalId AND c.deleted = false")
    long countByProjectExternalId(@Param("projectExternalId") String projectExternalId);
}
