package kr.devport.api.domain.newsletter.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyDigestSchedulerTest {

    private final WeeklyDigestService service = mock(WeeklyDigestService.class);
    private final WeeklyDigestScheduler scheduler = new WeeklyDigestScheduler(service);

    /** cron이 다이제스트 기간 경계(월 08:00 KST)에 정확히 맞아야 기간이 겹치거나 비지 않는다. */
    @Test
    void cronFiresExactlyAtWindowBoundary() throws NoSuchMethodException {
        Scheduled scheduled = WeeklyDigestScheduler.class.getMethod("sendWeeklyDigest").getAnnotation(Scheduled.class);
        ZonedDateTime wednesday = ZonedDateTime.of(2026, 10, 7, 15, 0, 0, 0, WeeklyDigestWindow.KST);

        ZonedDateTime nextFire = CronExpression.parse(scheduled.cron())
            .next(wednesday.withZoneSameInstant(StringUtils.parseTimeZoneString(scheduled.zone()).toZoneId()));

        assertThat(nextFire.toInstant()).isEqualTo(WeeklyDigestWindow.endingAtOrBefore(wednesday).next().end().toInstant());
    }

    @Test
    void doesNothingWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.sendWeeklyDigest();

        verify(service, never()).createWeeklyIssue(any());
    }

    @Test
    void anotherInstanceWinningTheRaceIsNotAnError() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        when(service.createWeeklyIssue(any())).thenThrow(new DataIntegrityViolationException("uk_newsletter_issues_digest_week"));

        assertThatCode(scheduler::sendWeeklyDigest).doesNotThrowAnyException();
        verify(service).createWeeklyIssue(any());
    }
}
