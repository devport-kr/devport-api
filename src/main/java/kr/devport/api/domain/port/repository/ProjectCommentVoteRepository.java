package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.ProjectCommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectCommentVoteRepository extends JpaRepository<ProjectCommentVote, Long> {
    Optional<ProjectCommentVote> findByComment_ExternalIdAndUser_Id(String commentExternalId, Long userId);

    @Query("SELECT SUM(v.vote) FROM ProjectCommentVote v WHERE v.comment.externalId = :commentExternalId")
    Integer calculateVoteScore(@Param("commentExternalId") String commentExternalId);

    void deleteByComment_ExternalIdAndUser_Id(String commentExternalId, Long userId);
}
