package kr.devport.api.domain.newsletter.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * 주간 다이제스트 한 회가 다루는 기간: 직전 발송 시각(월 08:00 KST)부터 이번 발송 시각까지.
 * 기간이 겹치지도 비지도 않아서 모든 글이 정확히 한 회에만 후보가 된다.
 */
record WeeklyDigestWindow(ZonedDateTime start, ZonedDateTime end) {

    /** KST는 서머타임이 없어 고정 오프셋으로 충분하고, tz 데이터베이스(native image 포함 여부)에 의존하지 않는다. */
    static final ZoneOffset KST = ZoneOffset.ofHours(9);
    static final DayOfWeek SEND_DAY = DayOfWeek.MONDAY;
    static final LocalTime SEND_TIME = LocalTime.of(8, 0);

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("M/d");

    /** now 이전의 가장 최근 발송 시각에 끝나는 기간 (발송 시점에는 이번 회) */
    static WeeklyDigestWindow endingAtOrBefore(ZonedDateTime now) {
        ZonedDateTime local = now.withZoneSameInstant(KST);
        ZonedDateTime end = local.with(TemporalAdjusters.previousOrSame(SEND_DAY)).with(SEND_TIME);
        if (end.isAfter(local)) {
            end = end.minusWeeks(1);
        }
        return new WeeklyDigestWindow(end.minusWeeks(1), end);
    }

    WeeklyDigestWindow next() {
        return new WeeklyDigestWindow(end, end.plusWeeks(1));
    }

    /** articles.created_at은 UTC로 저장된다 (크롤러 datetime.utcnow(), 운영 컨테이너 TZ=UTC). */
    LocalDateTime utcStart() {
        return start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    LocalDateTime utcEnd() {
        return end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** 기간이 시작하는 주의 ISO 주차 (예: 2026-W40) */
    String weekKey() {
        LocalDate date = start.toLocalDate();
        return String.format("%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** 제목/본문에 보여줄 기간 (예: 9/29~10/5) */
    String label() {
        return LABEL_FORMAT.format(start) + "~" + LABEL_FORMAT.format(end.minusDays(1));
    }
}
