package kr.devport.api.domain.newsletter.service.admin;

import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.event.NewsletterIssueCreatedEvent;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import kr.devport.api.domain.newsletter.service.NewsletterMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ACTIVE 구독자에게 뉴스레터를 배치 단위로 발송한다.
 * 긴 트랜잭션을 잡지 않도록 배치 조회/진행 상황 저장을 각각 짧은 트랜잭션으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterDispatcher {

    private final NewsletterIssueRepository issueRepository;
    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final NewsletterMailService mailService;

    @Value("${app.newsletter.send-batch-size:10}")
    private int batchSize;

    @Value("${app.newsletter.send-batch-interval-ms:1000}")
    private long batchIntervalMs;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCreated(NewsletterIssueCreatedEvent event) {
        dispatch(event.issueId());
    }

    public void dispatch(Long issueId) {
        NewsletterIssue issue = issueRepository.findById(issueId).orElse(null);
        if (issue == null || issue.getStatus() != NewsletterIssueStatus.SENDING) {
            log.warn("Newsletter dispatch skipped, issueId={} not found or not SENDING", issueId);
            return;
        }

        int sent = 0;
        int failed = 0;
        long lastId = 0L;
        boolean aborted = false;
        try {
            while (true) {
                List<NewsletterSubscription> batch = subscriptionRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
                    NewsletterSubscriptionStatus.ACTIVE, lastId, Limit.of(batchSize));
                if (batch.isEmpty()) {
                    break;
                }
                lastId = batch.getLast().getId();

                int batchFailed = mailService.sendIssueBatch(
                    issue.getSubject(), issue.getContent(), issue.getContentHtml(), batch);
                failed += batchFailed;
                sent += batch.size() - batchFailed;

                issue.setSentCount(sent);
                issue.setFailedCount(failed);
                issue = issueRepository.save(issue);

                if (batch.size() < batchSize) {
                    break;
                }
                Thread.sleep(batchIntervalMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            aborted = true;
            log.warn("Newsletter dispatch interrupted, issueId={}", issueId);
        } catch (Exception e) {
            aborted = true;
            log.error("Newsletter dispatch aborted, issueId={}", issueId, e);
        }

        issue.setSentCount(sent);
        issue.setFailedCount(failed);
        if (!aborted) {
            issue.setRecipientCount(sent + failed);
        }
        issue.setStatus(resolveStatus(sent, failed, aborted));
        issue.setCompletedAt(LocalDateTime.now());
        issueRepository.save(issue);
        log.info("Newsletter dispatch finished, issueId={}, sent={}, failed={}", issueId, sent, failed);
    }

    static NewsletterIssueStatus resolveStatus(int sent, int failed, boolean aborted) {
        if (failed == 0 && !aborted) {
            return NewsletterIssueStatus.SENT;
        }
        return sent == 0 ? NewsletterIssueStatus.FAILED : NewsletterIssueStatus.PARTIALLY_FAILED;
    }
}
