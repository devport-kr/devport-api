package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.dto.request.PasswordChangeRequest;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
            userRepository,
            passwordEncoder,
            emailVerificationService,
            emailService,
            refreshTokenService
        );
    }

    @Test
    void changePasswordRevokesExistingRefreshTokens() {
        User user = User.builder()
            .id(1L)
            .username("tester")
            .authProvider(AuthProvider.local)
            .password("encoded-current")
            .build();
        PasswordChangeRequest request = PasswordChangeRequest.builder()
            .currentPassword("Current@123")
            .newPassword("NewPass@123")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@123", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded-new");

        profileService.changePassword(1L, request);

        verify(userRepository).save(user);
        verify(refreshTokenService).deleteByUser(user);
    }
}
