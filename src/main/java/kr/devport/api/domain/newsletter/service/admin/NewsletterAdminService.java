package kr.devport.api.domain.newsletter.service.admin;

import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.newsletter.dto.request.admin.NewsletterIssueRequest;
import kr.devport.api.domain.newsletter.dto.request.admin.NewsletterTestSendRequest;
import kr.devport.api.domain.newsletter.dto.response.admin.NewsletterIssueResponse;
import kr.devport.api.domain.newsletter.dto.response.admin.NewsletterStatsResponse;
import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.event.NewsletterIssueCreatedEvent;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import kr.devport.api.domain.newsletter.service.NewsletterMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterAdminService {

    /** 발송 중 서버가 재시작되면 SENDING이 남을 수 있어, 최근 건만 중복 발송 차단에 사용한다. */
    private static final Duration SENDING_LOCK_WINDOW = Duration.ofHours(1);

    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final NewsletterIssueRepository issueRepository;
    private final NewsletterMailService mailService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NewsletterStatsResponse getStats() {
        return NewsletterStatsResponse.builder()
            .activeCount(subscriptionRepository.countByStatus(NewsletterSubscriptionStatus.ACTIVE))
            .pendingCount(subscriptionRepository.countByStatus(NewsletterSubscriptionStatus.PENDING))
            .build();
    }

    @Transactional(readOnly = true)
    public Page<NewsletterIssueResponse> getIssues(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return issueRepository.findAll(pageRequest).map(NewsletterIssueResponse::from);
    }

    public void sendTest(NewsletterTestSendRequest request) {
        mailService.sendTestIssue(request.getEmail().trim(), request.getSubject(), request.getContent(), null);
    }

    /** 발송 이력을 만들고, 커밋 후 비동기로 ACTIVE 구독자 전체에게 발송한다. */
    @Transactional
    public NewsletterIssueResponse createIssue(Long adminUserId, NewsletterIssueRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (issueRepository.existsByStatusAndCreatedAtAfter(NewsletterIssueStatus.SENDING, now.minus(SENDING_LOCK_WINDOW))) {
            throw new TooManyRequestsException("이전 뉴스레터를 발송 중입니다. 완료 후 다시 시도해주세요.");
        }

        NewsletterIssue issue = issueRepository.save(NewsletterIssue.builder()
            .subject(request.getSubject().strip())
            .content(request.getContent())
            .status(NewsletterIssueStatus.SENDING)
            .recipientCount((int) subscriptionRepository.countByStatus(NewsletterSubscriptionStatus.ACTIVE))
            .createdByUserId(adminUserId)
            .createdAt(now)
            .build());

        eventPublisher.publishEvent(new NewsletterIssueCreatedEvent(issue.getId()));
        log.info("Newsletter issue created, issueId={}, recipients={}", issue.getId(), issue.getRecipientCount());
        return NewsletterIssueResponse.from(issue);
    }
}
