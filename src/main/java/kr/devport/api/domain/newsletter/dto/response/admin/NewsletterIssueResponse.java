package kr.devport.api.domain.newsletter.dto.response.admin;

import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterIssueResponse {

    private Long id;
    private String subject;
    private String content;
    private NewsletterIssueStatus status;
    /** 주간 다이제스트면 다루는 주 (예: 2026-W40), 관리자 발송이면 null */
    private String digestWeek;
    private Integer recipientCount;
    private Integer sentCount;
    private Integer failedCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static NewsletterIssueResponse from(NewsletterIssue issue) {
        return NewsletterIssueResponse.builder()
            .id(issue.getId())
            .subject(issue.getSubject())
            .content(issue.getContent())
            .status(issue.getStatus())
            .digestWeek(issue.getDigestWeek())
            .recipientCount(issue.getRecipientCount())
            .sentCount(issue.getSentCount())
            .failedCount(issue.getFailedCount())
            .createdAt(issue.getCreatedAt())
            .completedAt(issue.getCompletedAt())
            .build();
    }
}
