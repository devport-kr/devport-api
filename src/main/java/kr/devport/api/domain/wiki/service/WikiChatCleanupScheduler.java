package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup for expired wiki chat sessions.
 * Runs nightly at 2 AM. Cascade delete in DB removes associated messages automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiChatCleanupScheduler {

    private final WikiChatSessionRepository sessionRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("wiki-cleanup: Starting expired session cleanup at {}", now);
        sessionRepository.deleteByExpiresAtBefore(now);
        log.info("wiki-cleanup: Expired session cleanup completed");
    }
}
