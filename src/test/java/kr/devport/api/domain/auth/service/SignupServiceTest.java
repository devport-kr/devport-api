package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.dto.request.SignupRequest;
import kr.devport.api.domain.auth.dto.response.AuthResponse;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.BotVerificationException;
import kr.devport.api.domain.common.exception.DuplicateUsernameException;
import kr.devport.api.domain.common.exception.InvalidTermsAgreementException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignupServiceTest {

    private static final String CLIENT_IP = "203.0.113.7";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TurnstileService turnstileService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RedisRateLimiter rateLimiter;

    private SignupService signupService;

    @BeforeEach
    void setUp() {
        signupService = new SignupService(
            userRepository,
            passwordEncoder,
            turnstileService,
            new TermsVersionPolicy("2026-03-24", List.of()),
            jwtTokenProvider,
            refreshTokenService,
            rateLimiter
        );
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(turnstileService.validateToken("turnstile-token", CLIENT_IP)).thenReturn(true);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3_600_000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh-token");
    }

    private SignupRequest request(String username, String termsVersion) {
        return SignupRequest.builder()
            .username(username)
            .password("Password@123")
            .agreedTermsVersion(termsVersion)
            .turnstileToken("turnstile-token")
            .build();
    }

    @Test
    void signupCreatesLocalAccountWithoutEmailAndLogsIn() {
        AuthResponse response = signupService.signup(request("tester", "2026-03-24"), CLIENT_IP);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("tester");
        assertThat(savedUser.getName()).isEqualTo("tester");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getEmail()).isNull();
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.local);
        assertThat(savedUser.getAgreedTermsVersion()).isEqualTo("2026-03-24");
        assertThat(savedUser.getAgreedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        verify(turnstileService).validateToken("turnstile-token", CLIENT_IP);
    }

    @Test
    void signupRejectsStaleTermsVersion() {
        assertThatThrownBy(() -> signupService.signup(request("tester", "2026-03-01"), CLIENT_IP))
            .isInstanceOf(InvalidTermsAgreementException.class)
            .hasMessage("You must agree to the current terms version to sign up");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void signupRejectsUsernameTakenIgnoringCase() {
        when(userRepository.existsByUsernameIgnoreCase("Tester")).thenReturn(true);

        assertThatThrownBy(() -> signupService.signup(request("Tester", "2026-03-24"), CLIENT_IP))
            .isInstanceOf(DuplicateUsernameException.class);
        verify(turnstileService, never()).validateToken(anyString(), anyString());
    }

    @Test
    void signupRejectsReservedUsername() {
        assertThatThrownBy(() -> signupService.signup(request("Admin", "2026-03-24"), CLIENT_IP))
            .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void signupRejectsFailedBotVerification() {
        when(turnstileService.validateToken("turnstile-token", CLIENT_IP)).thenReturn(false);

        assertThatThrownBy(() -> signupService.signup(request("tester", "2026-03-24"), CLIENT_IP))
            .isInstanceOf(BotVerificationException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void signupRejectsWhenIpRateLimitExceeded() {
        when(rateLimiter.tryAcquire(eq("signup:ip:" + CLIENT_IP), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> signupService.signup(request("tester", "2026-03-24"), CLIENT_IP))
            .isInstanceOf(TooManyRequestsException.class);
        verify(turnstileService, never()).validateToken(anyString(), anyString());
    }

    @Test
    void signupMapsUniqueConstraintRaceToDuplicateUsername() {
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> signupService.signup(request("tester", "2026-03-24"), CLIENT_IP))
            .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void usernameAvailabilityChecksFormatReservedAndExisting() {
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        assertThat(signupService.isUsernameAvailable("fresh_user-1")).isTrue();
        assertThat(signupService.isUsernameAvailable("taken")).isFalse();
        assertThat(signupService.isUsernameAvailable("root")).isFalse();
        assertThat(signupService.isUsernameAvailable("ab")).isFalse();
        assertThat(signupService.isUsernameAvailable("has space")).isFalse();
        assertThat(signupService.isUsernameAvailable(null)).isFalse();
    }
}
