package kr.devport.api.domain.wiki.service

import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Scheduled cleanup for expired wiki chat sessions. Runs nightly at 2 AM. DB cascade delete removes
 * associated messages automatically.
 */
@Component
class WikiChatCleanupScheduler(
    private val sessionRepository: WikiChatSessionRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun deleteExpiredSessions() {
        val now = LocalDateTime.now()
        log.info("wiki-cleanup: Starting expired session cleanup at {}", now)
        sessionRepository.deleteByExpiresAtBefore(now)
        log.info("wiki-cleanup: Expired session cleanup completed")
    }
}
