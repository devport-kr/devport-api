package kr.devport.api.service;

import kr.devport.api.domain.entity.RefreshToken;
import kr.devport.api.domain.entity.User;
import kr.devport.api.repository.RefreshTokenRepository;
import kr.devport.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        String token = jwtTokenProvider.generateRefreshToken(user.getId());
        long expirationMs = jwtTokenProvider.getRefreshTokenExpirationMs();

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000))
            .createdAt(LocalDateTime.now())
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional(readOnly = true)
    public boolean verifyRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isValid()) {
            return true;
        }

        refreshTokenRepository.delete(refreshToken);
        return false;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
