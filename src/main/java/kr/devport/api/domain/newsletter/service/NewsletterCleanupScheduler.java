package kr.devport.api.domain.newsletter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 03:30에 인증 기한이 지난 미인증 구독 신청(이메일)을 파기한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterCleanupScheduler {

    private final NewsletterSubscriptionService subscriptionService;

    @Scheduled(cron = "0 30 3 * * *")
    public void deleteExpiredPendingSubscriptions() {
        int deleted = subscriptionService.deleteExpiredPendingSubscriptions();
        log.info("newsletter-cleanup: deleted {} expired pending subscriptions", deleted);
    }
}
