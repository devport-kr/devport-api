package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.dto.request.SignupRequest;
import kr.devport.api.domain.auth.dto.response.AuthResponse;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.BotVerificationException;
import kr.devport.api.domain.common.exception.DuplicateUsernameException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 아이디/비밀번호만으로 LOCAL 계정을 만든다. 이메일은 받지 않으며 가입 즉시 로그인된다.
 * 이메일 인증이 없으므로 Turnstile 서버 검증과 IP 단위 rate limit으로 봇 가입을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    private static final Set<String> RESERVED_USERNAMES = Set.of(
        "admin", "administrator", "root", "system", "devport", "support", "help",
        "official", "staff", "moderator", "operator", "anonymous", "null", "undefined"
    );
    private static final int SIGNUP_LIMIT_PER_IP = 10;
    private static final Duration SIGNUP_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TurnstileService turnstileService;
    private final TermsVersionPolicy termsVersionPolicy;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RedisRateLimiter rateLimiter;

    @Transactional
    public AuthResponse signup(SignupRequest request, String clientIp) {
        termsVersionPolicy.requireAccepted(request.getAgreedTermsVersion());

        String username = request.getUsername();
        if (!isUsernameAvailable(username)) {
            throw new DuplicateUsernameException("Username is not available: " + username);
        }

        if (!rateLimiter.tryAcquire("signup:ip:" + clientIp, SIGNUP_LIMIT_PER_IP, SIGNUP_WINDOW)) {
            log.warn("Signup rate limit exceeded, clientIp={}", LogSanitizer.maskIp(clientIp));
            throw new TooManyRequestsException("회원가입 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        if (!turnstileService.validateToken(request.getTurnstileToken(), clientIp)) {
            log.warn("Turnstile validation failed for local signup, clientIp={}", LogSanitizer.maskIp(clientIp));
            throw new BotVerificationException("Bot verification failed");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(request.getPassword()))
            .name(username)
            .authProvider(AuthProvider.local)
            .role(UserRole.USER)
            .emailVerified(false)
            .createdAt(now)
            .updatedAt(now)
            .lastLoginAt(now)
            .agreedTermsVersion(request.getAgreedTermsVersion())
            .agreedAt(now)
            .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // 동시 가입 경합으로 unique 제약에 걸린 경우
            throw new DuplicateUsernameException("Username is not available: " + username);
        }
        log.info("User signup completed for userId={}", user.getId());

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .build();
    }

    public boolean isUsernameAvailable(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        if (RESERVED_USERNAMES.contains(username.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return !userRepository.existsByUsernameIgnoreCase(username);
    }
}
