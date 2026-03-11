package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.PasswordResetToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.PasswordResetTokenRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
            tokenRepository,
            userRepository,
            emailService,
            passwordEncoder,
            refreshTokenService
        );
    }

    @Test
    void resetPasswordRevokesExistingRefreshTokens() {
        User user = User.builder().id(1L).username("tester").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token("reset-token")
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .createdAt(LocalDateTime.now())
            .used(false)
            .build();

        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded-password");

        passwordResetService.resetPassword("reset-token", "NewPass@123");

        verify(userRepository).save(user);
        verify(refreshTokenService).deleteByUser(user);
        verify(tokenRepository).save(resetToken);
    }
}
