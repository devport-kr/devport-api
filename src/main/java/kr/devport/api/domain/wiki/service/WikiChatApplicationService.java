package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.request.WikiGlobalChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WikiChatApplicationService {

    private final WikiChatService wikiChatService;
    private final WikiGlobalChatService wikiGlobalChatService;
    private final WikiChatRateLimiter rateLimiter;
    private final WikiAnonRateLimiter anonRateLimiter;
    private final UserRepository userRepository;

    @Value("${wiki.chat.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public WikiChatResponse chatProject(
            String projectExternalId,
            WikiChatRequest request,
            Long userId,
            String clientIp
    ) {
        User user = resolveUser(userId, clientIp);
        WikiChatResult result = wikiChatService.chatResult(
                request.getSessionId(),
                projectExternalId,
                request.getQuestion(),
                user
        );
        return WikiChatResponse.from(result, request.getSessionId());
    }

    public WikiChatResult streamProject(
            String projectExternalId,
            WikiChatRequest request,
            Long userId,
            String clientIp,
            Consumer<String> tokenConsumer
    ) {
        User user = resolveUser(userId, clientIp);
        return wikiChatService.streamChatResult(
                request.getSessionId(),
                projectExternalId,
                request.getQuestion(),
                tokenConsumer,
                user
        );
    }

    public WikiGlobalChatResponse chatGlobal(
            WikiGlobalChatRequest request,
            Long userId,
            String clientIp
    ) {
        User user = resolveUser(userId, clientIp);
        return wikiGlobalChatService.chatResult(
                request.getSessionId(),
                request.getQuestion(),
                user
        );
    }

    public WikiGlobalChatResponse streamGlobal(
            WikiGlobalChatRequest request,
            Long userId,
            String clientIp,
            Consumer<String> tokenConsumer
    ) {
        User user = resolveUser(userId, clientIp);
        return wikiGlobalChatService.streamChatResult(
                request.getSessionId(),
                request.getQuestion(),
                tokenConsumer,
                user
        );
    }

    public void clearProjectSession(String sessionId) {
        wikiChatService.clearSession(sessionId);
    }

    private User resolveUser(Long userId, String clientIp) {
        if (userId == null) {
            if (rateLimitEnabled) {
                anonRateLimiter.checkAndIncrement(clientIp);
            }
            return null;
        }
        if (rateLimitEnabled) {
            rateLimiter.check(userId.toString());
        }
        return userRepository.findById(userId).orElse(null);
    }
}
