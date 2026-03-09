package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.wiki.entity.WikiChatSession;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WikiChatSessionRepository extends JpaRepository<WikiChatSession, Long> {

    Optional<WikiChatSession> findByExternalId(String externalId);

    Page<WikiChatSession> findByUserOrderByLastMessageAtDesc(User user, Pageable pageable);

    Page<WikiChatSession> findByUserAndProjectExternalIdOrderByLastMessageAtDesc(
            User user, String projectExternalId, Pageable pageable);

    Page<WikiChatSession> findByUserAndSessionTypeOrderByLastMessageAtDesc(
            User user, WikiChatSessionType sessionType, Pageable pageable);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
