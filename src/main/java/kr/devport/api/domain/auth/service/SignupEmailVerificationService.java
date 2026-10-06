package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.BotVerificationException;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.EmailVerificationCodeException;
import kr.devport.api.domain.common.exception.ServiceUnavailableException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.common.web.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * 아이디/비밀번호 회원가입 전 이메일 소유 확인.
 * 1) 6자리 인증번호를 메일로 보내고, 2) 맞게 입력하면 회원가입 요청에 쓸 1회용 인증 토큰을 준다.
 * 인증번호와 토큰은 Redis에 해시로만 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupEmailVerificationService {

    public static final Duration CODE_TTL = Duration.ofMinutes(10);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(30);
    static final int MAX_ATTEMPTS = 5;
    private static final Duration HOURLY = Duration.ofHours(1);
    private static final Duration DAILY = Duration.ofHours(24);
    private static final int CODE_LIMIT_PER_IP_PER_HOUR = 10;
    private static final int CODE_LIMIT_PER_EMAIL_PER_DAY = 5;
    private static final String KEY_PREFIX = "auth:signup:email:";
    private static final String TOO_MANY_MESSAGE = "인증번호 요청이 너무 많습니다. 1시간 후 다시 시도해주세요.";
    private static final String EXPIRED_CODE_MESSAGE = "인증번호가 만료되었습니다. 인증번호를 다시 요청해주세요.";
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "인증번호를 너무 많이 틀렸습니다. 인증번호를 다시 요청해주세요.";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TurnstileService turnstileService;
    private final RedisRateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 사이트 전체 인증번호 메일 시간당 상한. 공격 시 SES 평판을 지킨다. */
    @Value("${app.auth.signup-code-global-limit-per-hour:200}")
    private int globalLimitPerHour;

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void sendCode(String rawEmail, String turnstileToken, String clientIp) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        String emailHash = sha256(email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(emailHash)))) {
            throw new TooManyRequestsException("인증번호는 1분에 한 번만 보낼 수 있습니다. 잠시 후 다시 시도해주세요.");
        }

        if (!turnstileService.validateToken(turnstileToken, clientIp)) {
            log.warn("Turnstile validation failed for signup email code, clientIp={}", LogSanitizer.maskIp(clientIp));
            throw new BotVerificationException("Bot verification failed");
        }

        // 한도는 실제로 메일이 나간 요청만 소모해야 하므로, 이후 단계가 실패하면 획득한 permit을 모두 되돌린다.
        List<String> acquiredPermits = new ArrayList<>(3);
        try {
            if (ClientIpResolver.isPublicAddress(clientIp)) {
                // 사설 IP(프록시 주소)로 보이면 모든 사용자가 한 한도를 나눠 쓰게 되므로 건너뛴다.
                acquirePermit(acquiredPermits, "signup-code:ip:" + clientIp, CODE_LIMIT_PER_IP_PER_HOUR, HOURLY, TOO_MANY_MESSAGE);
            }
            acquirePermit(acquiredPermits, "signup-code:email:" + emailHash, CODE_LIMIT_PER_EMAIL_PER_DAY, DAILY,
                "이 이메일로 인증번호를 너무 많이 보냈습니다. 내일 다시 시도해주세요.");
            try {
                acquirePermit(acquiredPermits, "signup-code:global", globalLimitPerHour, HOURLY,
                    "지금은 인증번호 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            } catch (TooManyRequestsException e) {
                log.warn("Signup email code global limit ({}/h) reached — possible abuse", globalLimitPerHour);
                throw e;
            }

            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            ops().set(codeKey(emailHash), sha256(code), CODE_TTL);
            redisTemplate.delete(attemptsKey(emailHash));

            emailService.sendSignupCode(email, code, CODE_TTL.toMinutes());
        } catch (RuntimeException e) {
            acquiredPermits.forEach(rateLimiter::release);
            throw e;
        }
        ops().set(cooldownKey(emailHash), "1", RESEND_COOLDOWN);
        log.info("Signup email code sent to {}", LogSanitizer.maskEmail(email));
    }

    /**
     * 인증번호가 맞으면 회원가입에 쓸 1회용 인증 토큰을 돌려준다.
     * 인증번호 하나당 {@value #MAX_ATTEMPTS}번까지 시도할 수 있고, 넘으면 인증번호를 지운다.
     */
    public String verifyCode(String rawEmail, String code) {
        String emailHash = sha256(normalizeEmail(rawEmail));
        String codeKey = codeKey(emailHash);
        String attemptsKey = attemptsKey(emailHash);

        if (!(ops().get(codeKey) instanceof String codeHash)) {
            throw new EmailVerificationCodeException(EXPIRED_CODE_MESSAGE);
        }

        Long attempts = ops().increment(attemptsKey);
        if (attempts == null) {
            throw new ServiceUnavailableException("일시적으로 인증할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        if (attempts == 1L) {
            redisTemplate.expire(attemptsKey, CODE_TTL);
        }
        if (attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(List.of(codeKey, attemptsKey));
            throw new EmailVerificationCodeException(TOO_MANY_ATTEMPTS_MESSAGE);
        }

        if (!MessageDigest.isEqual(sha256(code).getBytes(StandardCharsets.UTF_8), codeHash.getBytes(StandardCharsets.UTF_8))) {
            long remaining = MAX_ATTEMPTS - attempts;
            if (remaining == 0) {
                redisTemplate.delete(List.of(codeKey, attemptsKey));
                throw new EmailVerificationCodeException(TOO_MANY_ATTEMPTS_MESSAGE);
            }
            throw new EmailVerificationCodeException("인증번호가 올바르지 않습니다. (남은 시도 " + remaining + "회)");
        }

        redisTemplate.delete(List.of(codeKey, attemptsKey));
        String verificationToken = generateToken();
        ops().set(verificationTokenKey(verificationToken), normalizeEmail(rawEmail), VERIFICATION_TOKEN_TTL);
        return verificationToken;
    }

    /** 회원가입 직전: 인증 토큰이 이 이메일(정규화된 값)에 대해 발급된 것인지 확인한다. */
    public void requireVerified(String email, String verificationToken) {
        if (!(ops().get(verificationTokenKey(verificationToken)) instanceof String verifiedEmail)
            || !verifiedEmail.equals(email)) {
            throw new EmailVerificationCodeException("이메일 인증이 만료되었거나 올바르지 않습니다. 이메일을 다시 인증해주세요.");
        }
    }

    /** 가입이 끝난 뒤 인증 토큰을 폐기해 재사용을 막는다. */
    public void consume(String verificationToken) {
        redisTemplate.delete(verificationTokenKey(verificationToken));
    }

    private void acquirePermit(List<String> acquiredPermits, String key, int limit, Duration window, String deniedMessage) {
        switch (rateLimiter.acquire(key, limit, window)) {
            case ALLOWED -> acquiredPermits.add(key);
            case DENIED -> {
                // 거부된 시도의 INCR도 되돌려 카운터가 실제 발송 수를 나타내게 한다.
                rateLimiter.release(key);
                log.info("Signup email code rate-limited by key={}", key);
                throw new TooManyRequestsException(deniedMessage);
            }
            case UNAVAILABLE -> throw new ServiceUnavailableException("일시적으로 인증번호를 보낼 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private ValueOperations<String, Object> ops() {
        return redisTemplate.opsForValue();
    }

    private static String codeKey(String emailHash) {
        return KEY_PREFIX + "code:" + emailHash;
    }

    private static String attemptsKey(String emailHash) {
        return KEY_PREFIX + "attempts:" + emailHash;
    }

    private static String cooldownKey(String emailHash) {
        return KEY_PREFIX + "cooldown:" + emailHash;
    }

    private static String verificationTokenKey(String verificationToken) {
        return KEY_PREFIX + "verified:" + sha256(verificationToken);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
