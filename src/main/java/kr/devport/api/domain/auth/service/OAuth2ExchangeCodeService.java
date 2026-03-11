package kr.devport.api.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.InvalidTokenException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OAuth2ExchangeCodeService {

    private static final String KEY_PREFIX = "auth:oauth2:exchange:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.oauth2-exchange-ttl-seconds:60}")
    private long exchangeCodeTtlSeconds;

    public String createExchangeCode(User user, HttpServletRequest request) {
        String code = generateCode();
        OAuth2ExchangeCodePayload payload = OAuth2ExchangeCodePayload.builder()
            .userId(user.getId())
            .provider(user.getAuthProvider())
            .userAgentHash(hash(normalizeUserAgent(request.getHeader("User-Agent"))))
            .createdAt(LocalDateTime.now())
            .build();

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(buildKey(code), payload, Duration.ofSeconds(exchangeCodeTtlSeconds));
        return code;
    }

    public User consumeExchangeCode(String code, HttpServletRequest request) {
        Object stored = redisTemplate.opsForValue().getAndDelete(buildKey(code));
        if (!(stored instanceof OAuth2ExchangeCodePayload payload)) {
            throw new InvalidTokenException("OAuth2 exchange code is invalid or expired");
        }

        String presentedUserAgentHash = hash(normalizeUserAgent(request.getHeader("User-Agent")));
        if (!payload.getUserAgentHash().equals(presentedUserAgentHash)) {
            throw new InvalidTokenException("OAuth2 exchange code is invalid or expired");
        }

        return userRepository.findById(payload.getUserId())
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuth2ExchangeCodePayload {
        private Long userId;
        private AuthProvider provider;
        private String userAgentHash;
        private LocalDateTime createdAt;
    }
}
