package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.EmailVerificationToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.dto.request.SignupRequest;
import kr.devport.api.domain.auth.dto.response.SignupResponse;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.DuplicateUsernameException;
import kr.devport.api.domain.common.exception.InvalidTermsAgreementException;
import kr.devport.api.domain.auth.repository.EmailVerificationTokenRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.auth.current-terms-version}")
    private String currentTermsVersion;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateTermsAgreement(request.getAgreedTermsVersion());

        // Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username already exists: " + request.getUsername());
        }

        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists: " + request.getEmail());
        }

        // Create user
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .name(request.getName())
            .authProvider(AuthProvider.local)
            .role(UserRole.USER)
            .emailVerified(false)
            .emailAddedAt(now)
            .createdAt(now)
            .updatedAt(now)
            .agreedTermsVersion(request.getAgreedTermsVersion())
            .agreedAt(now)
            .build();

        user = userRepository.save(user);
        log.info("User signup completed for userId={}", user.getId());

        // Generate email verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .token(token)
            .user(user)
            .expiresAt(now.plusHours(24))
            .createdAt(now)
            .build();

        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user, token);

        return SignupResponse.builder()
            .message("Account created. Verify your email before logging in.")
            .requiresEmailVerification(true)
            .email(user.getEmail())
            .build();
    }

    private void validateTermsAgreement(String agreedTermsVersion) {
        try {
            LocalDate.parse(agreedTermsVersion);
        } catch (Exception ex) {
            throw new InvalidTermsAgreementException("Terms version must be a valid date in YYYY-MM-DD format");
        }

        if (!currentTermsVersion.equals(agreedTermsVersion)) {
            throw new InvalidTermsAgreementException("You must agree to the current terms version to sign up");
        }
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
