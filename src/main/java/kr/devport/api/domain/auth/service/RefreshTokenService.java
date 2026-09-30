package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.RefreshToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.RefreshTokenRepository;
import kr.devport.api.domain.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        return issueRefreshToken(user);
    }

    @Transactional
    public RefreshToken requireValidRefreshToken(String rawToken) {
        RefreshToken refreshToken = findByRawToken(rawToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));

        if (refreshToken.isValid()) {
            return refreshToken;
        }

        refreshTokenRepository.delete(refreshToken);
        throw new InvalidTokenException("Refresh token is invalid or expired");
    }

    @Transactional
    public String rotateRefreshToken(RefreshToken refreshToken) {
        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        return issueRefreshToken(user);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByRawToken(String rawToken) {
        return refreshTokenRepository.findByToken(hashToken(rawToken));
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        findByRawToken(rawToken).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    private String issueRefreshToken(User user) {
        String rawToken = generateOpaqueToken();
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(hashToken(rawToken))
            .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
            .createdAt(LocalDateTime.now())
            .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
