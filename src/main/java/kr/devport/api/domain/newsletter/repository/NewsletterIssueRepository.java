package kr.devport.api.domain.newsletter.repository;

import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NewsletterIssueRepository extends JpaRepository<NewsletterIssue, Long> {

    boolean existsByStatusAndCreatedAtAfter(NewsletterIssueStatus status, LocalDateTime createdAt);

    boolean existsByDigestWeek(String digestWeek);
}
