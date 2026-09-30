package kr.devport.api.domain.article.repository;

import kr.devport.api.domain.article.entity.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {

    Optional<ArticleComment> findByExternalId(String externalId);

    @Query("""
        SELECT c FROM ArticleComment c
        JOIN FETCH c.user
        WHERE c.article.externalId = :articleExternalId
        ORDER BY c.createdAt ASC
    """)
    List<ArticleComment> findAllByArticleExternalId(@Param("articleExternalId") String articleExternalId);

    @Query("SELECT COUNT(c) FROM ArticleComment c WHERE c.article.externalId = :articleExternalId AND c.deleted = false")
    long countByArticleExternalId(@Param("articleExternalId") String articleExternalId);

    boolean existsByParentComment_Id(Long parentId);
}
