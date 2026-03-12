package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.dto.request.LoginRequest;
import kr.devport.api.domain.auth.dto.response.AuthResponse;
import kr.devport.api.domain.common.exception.EmailVerificationRequiredException;
import kr.devport.api.domain.common.exception.InvalidCredentialsException;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Verify authProvider is LOCAL
        if (user.getAuthProvider() != AuthProvider.local) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailVerificationRequiredException("Email verification is required before login");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.debug("User login completed for userId={}", user.getId());

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .build();
    }
}
