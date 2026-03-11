package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.PasswordResetToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.common.exception.TokenExpiredException;
import kr.devport.api.domain.common.exception.TokenNotFoundException;
import kr.devport.api.domain.auth.repository.PasswordResetTokenRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void createResetToken(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for non-existent email");
            return;
        }

        // Only LOCAL users can reset password
        if (user.getAuthProvider() != AuthProvider.local) {
            log.info("Password reset requested for non-local account: {}", user.getEmail());
            return;
        }

        // Delete old tokens for this user
        tokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .used(false)
            .build();

        tokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user, token);

        log.info("Password reset token created for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new TokenNotFoundException("Invalid reset token"));

        if (!resetToken.isValid()) {
            if (resetToken.getUsed()) {
                throw new TokenExpiredException("Reset token has already been used");
            }
            throw new TokenExpiredException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired password reset tokens deleted");
    }
}
