package kr.devport.api.domain.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 뉴스레터 1회분(관리자 발송 또는 주간 다이제스트)과 발송 결과 */
@Entity
@Table(name = "newsletter_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 미리 렌더링한 HTML 본문. 없으면 content를 HTML로 변환해 쓴다. */
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    /** 주간 다이제스트가 다루는 주 (예: 2026-W40). 같은 주를 두 번 발송하지 않도록 unique. */
    @Column(name = "digest_week", length = 10, unique = true)
    private String digestWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsletterIssueStatus status;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private Integer recipientCount = 0;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
