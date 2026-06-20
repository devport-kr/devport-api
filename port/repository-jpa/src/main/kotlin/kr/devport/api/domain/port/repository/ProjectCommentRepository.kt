package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.ProjectComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ProjectCommentRepository : JpaRepository<ProjectComment, Long> {
    fun findByExternalId(externalId: String): Optional<ProjectComment>

    @Query(
        """
        SELECT c FROM ProjectComment c
        JOIN FETCH c.user
        WHERE c.project.externalId = :projectExternalId
        ORDER BY c.createdAt ASC
        """,
    )
    fun findAllByProjectExternalId(
        @Param("projectExternalId") projectExternalId: String,
    ): List<ProjectComment>

    @Query("SELECT COUNT(c) FROM ProjectComment c WHERE c.project.externalId = :projectExternalId AND c.deleted = false")
    fun countByProjectExternalId(
        @Param("projectExternalId") projectExternalId: String,
    ): Long
}
