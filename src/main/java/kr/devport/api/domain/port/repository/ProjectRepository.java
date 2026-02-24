package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.Project;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {
    Optional<Project> findByExternalId(String externalId);
    List<Project> findByPort_Slug(String portSlug, Sort sort);

    @Query("""
            select p
            from Project p
            join fetch p.port
            order by p.stars desc, p.fullName asc
            """)
    List<Project> findAllForWikiAdmin();
}
