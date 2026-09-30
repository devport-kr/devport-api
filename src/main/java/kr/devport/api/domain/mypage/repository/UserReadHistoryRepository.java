package kr.devport.api.domain.mypage.repository;

import kr.devport.api.domain.mypage.entity.UserReadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {

    @Query("""
        SELECT h FROM UserReadHistory h
        JOIN FETCH h.article
        WHERE h.user.id = :userId
        ORDER BY h.readAt DESC
    """)
    Page<UserReadHistory> findByUserIdOrderByReadAtDesc(
        @Param("userId") Long userId, Pageable pageable);

    Optional<UserReadHistory> findByUserIdAndArticle_Id(Long userId, Long articleId);
}
