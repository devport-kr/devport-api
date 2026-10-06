package kr.devport.api.domain.newsletter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * 매주 월요일 08:00(KST)에 지난주 다이제스트를 발송한다.
 * 운영에서만 켠다 (app.newsletter.weekly-digest.enabled). AOT가 조건을 빌드 시점에 고정하므로 @ConditionalOnProperty 대신 실행 시 확인한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyDigestScheduler {

    private final WeeklyDigestService weeklyDigestService;

    @Value("${app.newsletter.weekly-digest.enabled:false}")
    private boolean enabled;

    // GMT+09:00 = KST. WeeklyDigestWindow와 같이 tz 데이터베이스 없이 고정 오프셋을 쓴다.
    @Scheduled(cron = "0 0 8 * * MON", zone = "GMT+09:00")
    public void sendWeeklyDigest() {
        if (!enabled) {
            log.info("weekly-digest: disabled, skipping");
            return;
        }
        try {
            weeklyDigestService.createWeeklyIssue(ZonedDateTime.now(WeeklyDigestWindow.KST));
        } catch (DataIntegrityViolationException e) {
            log.info("weekly-digest: already created by another instance");
        }
    }
}
