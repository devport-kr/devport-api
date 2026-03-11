package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.EmailVerificationToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.common.exception.TokenExpiredException;
import kr.devport.api.domain.common.exception.TokenNotFoundException;
import kr.devport.api.domain.auth.repository.EmailVerificationTokenRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public EmailVerificationToken createVerificationToken(User user) {
        // Delete old tokens for this user
        tokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .createdAt(LocalDateTime.now())
            .build();

        return tokenRepository.save(verificationToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new TokenNotFoundException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete the token after successful verification
        tokenRepository.delete(verificationToken);

        log.info("Email verified for user: {}", user.getUsername());
    }

    @Transactional
    public void resendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        EmailVerificationToken token = createVerificationToken(user);
        emailService.sendVerificationEmail(user, token.getToken());

        log.info("Verification email resent to: {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmailIfEligible(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getAuthProvider() != AuthProvider.local || Boolean.TRUE.equals(user.getEmailVerified())) {
                return;
            }

            EmailVerificationToken token = createVerificationToken(user);
            emailService.sendVerificationEmail(user, token.getToken());
            log.info("Verification email resent to eligible user: {}", user.getEmail());
        });
    }

    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired email verification tokens deleted");
    }
}
