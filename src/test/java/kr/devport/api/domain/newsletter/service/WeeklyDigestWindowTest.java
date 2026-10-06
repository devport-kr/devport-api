package kr.devport.api.domain.newsletter.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyDigestWindowTest {

    private static ZonedDateTime kst(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, WeeklyDigestWindow.KST);
    }

    @Test
    void atSendTimeCoversThePreviousSevenDays() {
        // 2026-10-05 is a Monday
        WeeklyDigestWindow window = WeeklyDigestWindow.endingAtOrBefore(kst(2026, 10, 5, 8, 0).plusNanos(5_000_000));

        assertThat(window.start()).isEqualTo(kst(2026, 9, 28, 8, 0));
        assertThat(window.end()).isEqualTo(kst(2026, 10, 5, 8, 0));
        assertThat(window.weekKey()).isEqualTo("2026-W40");
        assertThat(window.label()).isEqualTo("9/28~10/4");
    }

    @Test
    void beforeSendTimeOnMondayStillPointsAtTheLastSentWindow() {
        WeeklyDigestWindow window = WeeklyDigestWindow.endingAtOrBefore(kst(2026, 10, 5, 7, 59));

        assertThat(window.end()).isEqualTo(kst(2026, 9, 28, 8, 0));
    }

    @Test
    void nextIsTheWindowBeingFilledNow() {
        WeeklyDigestWindow current = WeeklyDigestWindow.endingAtOrBefore(kst(2026, 10, 7, 15, 0));

        assertThat(current.end()).isEqualTo(kst(2026, 10, 5, 8, 0));
        assertThat(current.next().start()).isEqualTo(current.end());
        assertThat(current.next().end()).isEqualTo(kst(2026, 10, 12, 8, 0));
    }

    @Test
    void queryBoundsAreUtcBecauseArticlesStoreUtc() {
        WeeklyDigestWindow window = WeeklyDigestWindow.endingAtOrBefore(kst(2026, 10, 5, 8, 0));

        assertThat(window.utcStart()).isEqualTo(LocalDateTime.of(2026, 9, 27, 23, 0));
        assertThat(window.utcEnd()).isEqualTo(LocalDateTime.of(2026, 10, 4, 23, 0));
    }

    @Test
    void weekKeyUsesIsoWeekBasedYear() {
        // window starting Monday 2025-12-29 belongs to ISO week 2026-W01
        WeeklyDigestWindow window = WeeklyDigestWindow.endingAtOrBefore(kst(2026, 1, 5, 9, 0));

        assertThat(window.weekKey()).isEqualTo("2026-W01");
        assertThat(window.label()).isEqualTo("12/29~1/4");
    }
}
