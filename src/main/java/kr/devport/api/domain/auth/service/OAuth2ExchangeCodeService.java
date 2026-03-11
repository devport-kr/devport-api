package kr.devport.api.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2ExchangeCodeService {

    private static final String KEY_PREFIX = "auth:oauth2:exchange:";
    private static final String USER_ID_KEY = "userId";
    private static final String USER_AGENT_HASH_KEY = "userAgentHash";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.oauth2-exchange-ttl-seconds:60}")
    private long exchangeCodeTtlSeconds;

    public String createExchangeCode(User user, HttpServletRequest request) {
        String code = generateCode();
        Map<String, Object> payload = Map.of(
            USER_ID_KEY, user.getId(),
            USER_AGENT_HASH_KEY, hash(normalizeUserAgent(request.getHeader("User-Agent")))
        );

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(buildKey(code), payload, Duration.ofSeconds(exchangeCodeTtlSeconds));
        return code;
    }

    public User consumeExchangeCode(String code, HttpServletRequest request) {
        Object stored = redisTemplate.opsForValue().getAndDelete(buildKey(code));
        if (!(stored instanceof Map<?, ?> payload)) {
            throw new InvalidTokenException("OAuth2 exchange code is invalid or expired");
        }

        Object userIdValue = payload.get(USER_ID_KEY);
        Object userAgentHashValue = payload.get(USER_AGENT_HASH_KEY);
        if (!(userIdValue instanceof Number userIdNumber) || !(userAgentHashValue instanceof String userAgentHash)) {
            throw new InvalidTokenException("OAuth2 exchange code is invalid or expired");
        }

        String presentedUserAgentHash = hash(normalizeUserAgent(request.getHeader("User-Agent")));
        if (!userAgentHash.equals(presentedUserAgentHash)) {
            throw new InvalidTokenException("OAuth2 exchange code is invalid or expired");
        }

        return userRepository.findById(userIdNumber.longValue())
            .orElseThrow(() -> new InvalidTokenException("OAuth2 exchange code is invalid or expired"));
    }

    private String buildKey(String code) {
        return KEY_PREFIX + code;
    }

    private String generateCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeUserAgent(String userAgent) {
        return userAgent == null ? "" : userAgent.trim();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
