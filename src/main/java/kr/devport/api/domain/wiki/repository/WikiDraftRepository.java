package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiDraftRepository extends JpaRepository<WikiDraft, Long> {

    List<WikiDraft> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    Optional<WikiDraft> findTopByProjectIdOrderByUpdatedAtDesc(Long projectId);

    Optional<WikiDraft> findByIdAndProjectId(Long id, Long projectId);
}
