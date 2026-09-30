package kr.devport.api.domain.mypage.repository;

import kr.devport.api.domain.mypage.entity.UserSavedArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSavedArticleRepository extends JpaRepository<UserSavedArticle, Long> {

    @Query("""
        SELECT s FROM UserSavedArticle s
        JOIN FETCH s.article
        WHERE s.user.id = :userId
        ORDER BY s.createdAt DESC
    """)
    Page<UserSavedArticle> findByUserIdOrderByCreatedAtDesc(
        @Param("userId") Long userId, Pageable pageable);

    Optional<UserSavedArticle> findByUserIdAndArticle_ExternalId(Long userId, String articleExternalId);

    boolean existsByUserIdAndArticle_ExternalId(Long userId, String articleExternalId);

    void deleteByUserIdAndArticle_ExternalId(Long userId, String articleExternalId);
}
