package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.Project
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ProjectRepository :
    JpaRepository<Project, Long>,
    ProjectRepositoryCustom {
    fun findByExternalId(externalId: String): Optional<Project>

    @Query(
        """
        select p
        from Project p
        order by p.stars desc, p.fullName asc
        """,
    )
    fun findAllForWikiAdmin(): List<Project>

    override fun findAll(sort: Sort): MutableList<Project>
}
