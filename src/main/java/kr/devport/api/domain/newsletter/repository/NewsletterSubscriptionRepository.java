package kr.devport.api.domain.newsletter.repository;

import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {

    Optional<NewsletterSubscription> findByUserId(Long userId);

    Optional<NewsletterSubscription> findByVerificationTokenHash(String verificationTokenHash);

    Optional<NewsletterSubscription> findByUnsubscribeToken(String unsubscribeToken);

    boolean existsByEmailAndStatusAndUserIdNot(String email, NewsletterSubscriptionStatus status, Long userId);

    long countByStatus(NewsletterSubscriptionStatus status);

    /** 발송용 keyset 페이지네이션 */
    List<NewsletterSubscription> findByStatusAndIdGreaterThanOrderByIdAsc(
        NewsletterSubscriptionStatus status,
        Long id,
        Limit limit
    );

    @Modifying
    @Query("DELETE FROM NewsletterSubscription s WHERE s.status = :status AND s.verificationExpiresAt < :before")
    int deleteByStatusAndVerificationExpiresAtBefore(
        @Param("status") NewsletterSubscriptionStatus status,
        @Param("before") LocalDateTime before
    );
}
