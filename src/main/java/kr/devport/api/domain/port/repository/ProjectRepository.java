package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.Project;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {
    Optional<Project> findByExternalId(String externalId);

    @Query("""
            select p
            from Project p
            order by p.stars desc, p.fullName asc
            """)
    List<Project> findAllForWikiAdmin();
}
