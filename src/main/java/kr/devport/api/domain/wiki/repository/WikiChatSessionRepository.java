package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.wiki.entity.WikiChatSession;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WikiChatSessionRepository extends JpaRepository<WikiChatSession, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<WikiChatSession> findByExternalId(String externalId);

    @EntityGraph(attributePaths = "user")
    Page<WikiChatSession> findByUserOrderByLastMessageAtDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<WikiChatSession> findByUserAndProjectExternalIdOrderByLastMessageAtDesc(
            User user, String projectExternalId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<WikiChatSession> findByUserAndSessionTypeOrderByLastMessageAtDesc(
            User user, WikiChatSessionType sessionType, Pageable pageable);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
