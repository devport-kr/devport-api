package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.dto.request.SignupRequest;
import kr.devport.api.domain.auth.dto.response.SignupResponse;
import kr.devport.api.domain.auth.entity.EmailVerificationToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.EmailVerificationTokenRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.InvalidTermsAgreementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private SignupService signupService;

    @BeforeEach
    void setUp() {
        signupService = new SignupService(
            userRepository,
            verificationTokenRepository,
            passwordEncoder,
            emailService
        );
        ReflectionTestUtils.setField(signupService, "currentTermsVersion", "2026-03-24");
    }

    @Test
    void signupPersistsTermsAgreementMetadata() {
        SignupRequest request = SignupRequest.builder()
            .username("tester")
            .password("Password@123")
            .email("tester@devport.kr")
            .name("Tester")
            .agreedTermsVersion("2026-03-24")
            .build();

        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(userRepository.existsByEmail("tester@devport.kr")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(verificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = signupService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getAgreedTermsVersion()).isEqualTo("2026-03-24");
        assertThat(savedUser.getAgreedAt()).isNotNull();
        assertThat(savedUser.getAgreedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(response.getEmail()).isEqualTo("tester@devport.kr");
        verify(emailService).sendVerificationEmail(any(User.class), any(String.class));
    }

    @Test
    void signupRejectsStaleTermsVersion() {
        SignupRequest request = SignupRequest.builder()
            .username("tester")
            .password("Password@123")
            .email("tester@devport.kr")
            .agreedTermsVersion("2026-03-01")
            .build();

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(InvalidTermsAgreementException.class)
            .hasMessage("You must agree to the current terms version to sign up");
    }
}
