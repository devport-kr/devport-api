package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.wiki.dto.response.WikiMessageResponse;
import kr.devport.api.domain.wiki.dto.response.WikiSessionListResponse;
import kr.devport.api.domain.wiki.dto.response.WikiSessionResponse;
import kr.devport.api.domain.wiki.entity.WikiChatMessage;
import kr.devport.api.domain.wiki.entity.WikiChatSession;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import kr.devport.api.domain.wiki.exception.WikiSessionNotFoundException;
import kr.devport.api.domain.wiki.repository.WikiChatMessageRepository;
import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles all DB persistence for wiki chat sessions and messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiChatSessionPersistenceService {

    private static final int SESSION_TTL_DAYS = 10;

    private final WikiChatSessionRepository sessionRepository;
    private final WikiChatMessageRepository messageRepository;

    /**
     * Find or create a session. Updates last_message_at and expiresAt on every call.
     */
    @Transactional
    public WikiChatSession findOrCreateSession(
            String externalId,
            User user,
            String projectExternalId,
            WikiChatSessionType sessionType
    ) {
        return sessionRepository.findByExternalId(externalId)
                .map(session -> {
                    LocalDateTime now = LocalDateTime.now();
                    session.setLastMessageAt(now);
                    session.setExpiresAt(now.plusDays(SESSION_TTL_DAYS));
                    return sessionRepository.save(session);
                })
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    WikiChatSession session = WikiChatSession.builder()
                            .externalId(externalId)
                            .user(user)
                            .projectExternalId(projectExternalId)
                            .sessionType(sessionType)
                            .lastMessageAt(now)
                            .expiresAt(now.plusDays(SESSION_TTL_DAYS))
                            .build();
                    return sessionRepository.save(session);
                });
    }

    /**
     * Save a user message to the DB.
     */
    @Transactional
    public WikiChatMessage saveUserMessage(WikiChatSession session, String question) {
        WikiChatMessage message = WikiChatMessage.builder()
                .session(session)
                .role("USER")
                .content(question)
                .isClarification(false)
                .build();
        return messageRepository.save(message);
    }

    /**
     * Save an assistant message to the DB.
     */
    @Transactional
    public WikiChatMessage saveAssistantMessage(WikiChatSession session, String answer, boolean isClarification) {
        WikiChatMessage message = WikiChatMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(answer)
                .isClarification(isClarification)
                .build();
        return messageRepository.save(message);
    }

    /**
     * Load recent messages from DB and convert to ChatTurn list (oldest-first).
     * Used as fallback when Redis session is cold (resumed session).
     */
    @Transactional(readOnly = true)
    public List<ChatTurn> loadRecentMessages(String externalId, int limit) {
        return sessionRepository.findByExternalId(externalId)
                .map(session -> {
                    List<WikiChatMessage> messages = messageRepository.findTop20BySessionOrderByCreatedAtDesc(session);
                    // Reverse to get chronological order
                    Collections.reverse(messages);
                    // Pair USER/ASSISTANT messages into turns
                    List<ChatTurn> turns = new ArrayList<>();
                    for (int i = 0; i < messages.size() - 1; i++) {
                        WikiChatMessage msg = messages.get(i);
                        WikiChatMessage next = messages.get(i + 1);
                        if ("USER".equals(msg.getRole()) && "ASSISTANT".equals(next.getRole())) {
                            turns.add(ChatTurn.builder()
                                    .question(msg.getContent())
                                    .answer(next.getContent())
                                    .wasClarification(next.isClarification())
                                    .build());
                            i++; // skip the assistant message
                        }
                    }
                    // Return last `limit` turns
                    if (turns.size() > limit) {
                        return turns.subList(turns.size() - limit, turns.size());
                    }
                    return turns;
                })
                .orElse(List.of());
    }

    /**
     * Load all messages for a session (auth-checked).
     */
    @Transactional(readOnly = true)
    public List<WikiMessageResponse> loadSessionMessages(String externalId, Long userId) {
        WikiChatSession session = sessionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new WikiSessionNotFoundException("세션을 찾을 수 없습니다."));

        if (session.getUser() == null || !session.getUser().getId().equals(userId)) {
            throw new WikiSessionNotFoundException("세션을 찾을 수 없습니다.");
        }

        return messageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(msg -> new WikiMessageResponse(
                        msg.getRole(),
                        msg.getContent(),
                        msg.isClarification(),
                        msg.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Get all sessions for a user (paginated).
     */
    @Transactional(readOnly = true)
    public WikiSessionListResponse getUserSessions(User user, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<WikiChatSession> sessionPage = sessionRepository.findByUserOrderByLastMessageAtDesc(user, pageable);
        return toListResponse(sessionPage);
    }

    /**
     * Get project sessions for a user (paginated).
     */
    @Transactional(readOnly = true)
    public WikiSessionListResponse getProjectSessions(User user, String projectExternalId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<WikiChatSession> sessionPage = sessionRepository
                .findByUserAndProjectExternalIdOrderByLastMessageAtDesc(user, projectExternalId, pageable);
        return toListResponse(sessionPage);
    }

    /**
     * Get global chat sessions for a user (paginated).
     */
    @Transactional(readOnly = true)
    public WikiSessionListResponse getGlobalSessions(User user, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<WikiChatSession> sessionPage = sessionRepository
                .findByUserAndSessionTypeOrderByLastMessageAtDesc(user, WikiChatSessionType.GLOBAL, pageable);
        return toListResponse(sessionPage);
    }

    /**
     * Hard delete a session (auth-checked). Cascade deletes messages automatically.
     */
    @Transactional
    public void deleteSession(String externalId, Long userId) {
        WikiChatSession session = sessionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new WikiSessionNotFoundException("세션을 찾을 수 없습니다."));

        if (session.getUser() == null || !session.getUser().getId().equals(userId)) {
            throw new WikiSessionNotFoundException("세션을 찾을 수 없습니다.");
        }

        sessionRepository.delete(session);
    }

    /**
     * Check if a session has 0 prior messages (first message check).
     */
    @Transactional(readOnly = true)
    public boolean isFirstMessage(String externalId) {
        return sessionRepository.findByExternalId(externalId)
                .map(session -> messageRepository.countBySession(session) == 0)
                .orElse(true);
    }

    private WikiSessionListResponse toListResponse(Page<WikiChatSession> sessionPage) {
        List<WikiChatSession> sessions = sessionPage.getContent();
        Map<Long, Long> countMap = sessions.isEmpty()
                ? Map.of()
                : messageRepository.countMapForSessions(sessions);

        List<WikiSessionResponse> responses = sessions.stream()
                .map(session -> new WikiSessionResponse(
                        session.getExternalId(),
                        session.getTitle(),
                        session.getSessionType().name(),
                        session.getProjectExternalId(),
                        session.getCreatedAt(),
                        session.getLastMessageAt(),
                        countMap.getOrDefault(session.getId(), 0L).intValue()
                ))
                .toList();

        return new WikiSessionListResponse(
                responses,
                sessionPage.getTotalPages(),
                sessionPage.getTotalElements(),
                sessionPage.getNumber(),
                sessionPage.getSize()
        );
    }
}
